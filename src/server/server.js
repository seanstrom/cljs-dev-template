// import * as server from '#cljs/server/server.main.js'

import { DOMParser } from "@xmldom/xmldom"

import * as ConcurrentTask from "@andrewmacmurray/elm-concurrent-task"
import express from 'express'
import core from "./global.js"
// import { Elm } from "./Server.elm"
import cors from "cors"
import superagent from "superagent"
import ElmWrapped from "#elm/server.elm.js"

const Elm = ElmWrapped(globalThis)

core.tasks = {
  spotifyEmbedHtml: async (playlistId) => {
    const url = `https://open.spotify.com/embed/playlist/${playlistId}`
    const response = await superagent.get(url)
    return response.text
  },
  spotifyEmbed: async (playlistId) => {
    const url = `https://open.spotify.com/embed/playlist/${playlistId}`
    const response = await superagent.get(url)
    const embedHtml = response.text
  
    const parser = new DOMParser();
    const doc = parser.parseFromString(embedHtml, "text/html");
    const element = doc.getElementById("__NEXT_DATA__");
    const embedProps = JSON.parse(element.textContent);
    const embedEntity = embedProps.props.pageProps.state.data.entity;

    return embedEntity
  }
}

function makeBridge(state) {
  return {
    routes: {
      "messageId": (response) => {
      }
    },

    sendRequest: ({ requestId, content }) => {
      const promise = new Promise((resolve, _reject) => {
        const route = requestId
        state.bridge.routes[route] = (state, response) => {
          resolve(response)
        }
      })
      state.elm?.ports?.inbox?.send({
        type: "request",
        requestId: requestId,
        content: content,
      })
      return promise
    },

    handleOutboxMessage: (state, message, callback) => {
      switch (message.tag) {
        case "response": {
          console.log("outbox message", message)
          const responseHandler = state.bridge.routes[message.requestId]
          if (responseHandler) {
            delete state.bridge.routes[message.requestId]
            responseHandler(state, message)
          } else {
            callback(state, message)
          }
          break
        }
        default: {
          callback(state, message)
          break
        }
      }
    },

    subscribe: (callback) => {
      state.elm?.ports?.outbox?.subscribe(message => {
        if (Array.isArray(message)) {
          const { responses, other } = message.reduce(
            (acc, msg) => {
              if (msg.tag === "response") {
                acc.responses.push(msg)
              } else {
                acc.other.push(msg)
              }
              return acc
            },
            {
              responses: [],
              other: []
            })
          responses.forEach(msg => {
            state.bridge.handleOutboxMessage(state, msg, callback)
          })
          state.bridge.handleOutboxMessage(state, other, callback)
        } else {
          state.bridge.handleOutboxMessage(state, message, callback)
        }
      })

      ConcurrentTask.register({
        tasks: {
          spotifyEmbed: async (playlistId) => {
            return await state.tasks.spotifyEmbed(playlistId)
          },
          respond: (message) => {
            switch (message.tag) {
              case "response": {
                const responseHandler = state.bridge.routes[message.requestId]
                if (responseHandler) {
                  delete state.bridge.routes[message.requestId]
                  responseHandler(state, message)
                } else {
                  // callback(state, message)
                }
                break
              }
              default: {
                // callback(state, message)
                break
              }
            }
          },
        },
        ports: {
          send: state.elm?.ports?.run,
          receive: state.elm?.ports?.track,
        },
      })
    },

    unsubscribe: () => {
      state.elm?.ports?.run?.unsubscribe()
      state.elm?.ports?.outbox?.unsubscribe()
    }
  }
}

function handleMessage(state, msg) {
  switch (msg.tag) {
    case "increment":
      state.count += 2
      return
  }
}

const ports = {
  outbox: (state, msg) => {
    if (Array.isArray(msg)) {
      msg.forEach(m => {
        handleMessage(state, m)
      })
    } else {
      handleMessage(state, msg)
    }
  }
}

const handlers = {
  hey: async (req, res) => {
    req.state.count += 2
    const response = await req.state.bridge.sendRequest({
      requestId: 1,
      content: {}
    })
    res.send(`current count ${core.count}`)
  },
  spotifyEmbed: async (req, res) => {
    const embedHtml = await req.state.tasks.spotifyEmbedHtml(req.query.playlistId)    
    res.json({ html: embedHtml })
  },
  backendMsg: async (req, res) => {
    const payload = await req.state.bridge.sendRequest({
      requestId: req.body.requestId,
      content: req.body.content,
    })
    res.json(payload)
  },
}

core.ports = ports
core.handlers = handlers

export const app = main(core)

function makeRouter(state) {
  const router = new express.Router()
  router.get('/hey', state.handlers.hey)
  router.get('/spotifyEmbed', state.handlers.spotifyEmbed)
  router.post('/backendMsg', express.json(), state.handlers.backendMsg)
  return router
}

function makeElmApp(state) {
  const elm = Elm.Server.init()
  return elm
}

function makeElmBridge(state) {
  if (state.bridge) {
    state.bridge.unsubscribe()
  }

  const bridge = makeBridge(state)
  bridge.subscribe(function outboxSub(state, msg) {
    state.ports.outbox(state, msg)
  })

  return bridge
}

function main(state = {}) {
  state.elm = makeElmApp(state)
  state.bridge = makeElmBridge(state)
  state.router = makeRouter(state)
  if (state.ready) return state.app

  const app = express()
  state.app = app

  app.use(cors())
  app.use(function router(req, res, next) {
    req.state = state
    state.router(req, res, next)
  })

  state.ready = true
  return app
}
