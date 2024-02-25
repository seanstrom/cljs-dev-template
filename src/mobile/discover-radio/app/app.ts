/*
In NativeScript, the app.ts file is the entry point to your application.
You can use this file to perform app-level initialization, but the primary
purpose of the file is to pass control to the app’s first page.
*/

import { Application } from '@nativescript/core'
import { document, globalRegister } from 'dominative'
import { Elm } from './Main.elm'

globalRegister(global)

const init = (node: any, flags: any) => {
    const app = Elm().Main.init({ node, flags })
    return {
        app,
        setup: () => { console.log("setup app") },
        teardown: () => { console.log("teardown app") },
    }
}

function boot(flags: any) {
    return new Promise((resolve, reject) => {
        let instance: any

        Application.on(Application.launchEvent, () => {
            console.log("launch before")
            console.log("launch after")
        })

        Application.on(Application.exitEvent, () => {
            instance.teardown()
            instance = null
        })

        try {
            Application.run({
                create: () => {
                    console.log("init")
                    instance = init(document.body, flags)
                    instance.setup()
                    resolve(instance)
                    console.log("init after")
                    return document
                },
            })
        } catch (e) {
            reject(e)
        }
    })
}

async function main() {
    await boot({})
}

main()
