module Main exposing (main)

import Browser
import ConcurrentTask exposing (ConcurrentTask)
import Html exposing (Html, button, div, img)
import Html.Attributes exposing (class, src, style)
import Html.Events exposing (onClick)
import Json.Decode as Decode
import Json.Encode as Encode
import Main.View
import Port
import Port.Hook
import Port.Item
import Port.Task
import Stuff.HelloWorld exposing (helloWorld)
import Stuff.Icons as Icons
import VitePluginHelper


type Msg
    = PortHook Port.Hook.Msg
    | PortTask Port.Task.Msg
    | MainView Main.View.WebMsg


type alias Model =
    { tasks :
        ConcurrentTask.Pool
            Port.Task.Msg
            Port.Task.Error
            Port.Task.Success
    , paused : Bool
    , count : Int
    }


bootSpotify : ConcurrentTask.ConcurrentTask x Port.Task.Success
bootSpotify =
    ConcurrentTask.define
        { function = "boot"
        , expect = ConcurrentTask.expectWhatever
        , errors = ConcurrentTask.expectNoErrors
        , args = Encode.null
        }
        |> ConcurrentTask.map Port.Task.Whatever


resumePlayer : ConcurrentTask.ConcurrentTask x Port.Task.Success
resumePlayer =
    ConcurrentTask.define
        { function = "resumePlayer"
        , expect = ConcurrentTask.expectJson Decode.bool
        , errors = ConcurrentTask.expectNoErrors
        , args = Encode.null
        }
        |> ConcurrentTask.map Port.Task.Playing


init : () -> ( Model, Cmd Msg )
init flags =
    let
        ( tasks, cmd ) =
            ConcurrentTask.attempt
                { send = Port.Task.run
                , pool = ConcurrentTask.pool
                , onComplete = Port.Task.OnComplete
                }
                bootSpotify
    in
    ( { tasks = tasks
      , paused = True
      , count = 0
      }
    , Cmd.map PortTask cmd
    )


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.batch
        [ Port.Hook.inbox <|
            Port.receive PortHook
        , Sub.map PortTask <|
            ConcurrentTask.onProgress
                { send = Port.Task.run
                , receive = Port.Task.track
                , onProgress = Port.Task.OnProgress
                }
                model.tasks
        ]


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        MainView Main.View.PrevTrack ->
            ( model
            , Port.send <|
                Port.Hook.Payload
                    { tag = "action:previousTrack"
                    , value = Encode.null
                    }
            )

        MainView Main.View.NextTrack ->
            ( model
            , Port.send <|
                Port.Hook.Payload
                    { tag = "action:nextTrack"
                    , value = Encode.null
                    }
            )

        MainView Main.View.Resume ->
            let
                ( tasks, cmd ) =
                    ConcurrentTask.attempt
                        { send = Port.Task.run
                        , pool = model.tasks
                        , onComplete = Port.Task.OnComplete
                        }
                        resumePlayer
            in
            ( { model
                | paused = False
                , tasks = tasks
              }
            , Cmd.map PortTask cmd
            )

        MainView Main.View.Pause ->
            ( { model | paused = True }
            , Port.send <|
                Port.Hook.Payload
                    { tag = "action:pause"
                    , value = Encode.null
                    }
            )

        PortHook _ ->
            let
                _ =
                    Debug.log "IncomingMessage" ( model.paused, Cmd.none )
            in
            ( model, Cmd.none )

        PortTask (Port.Task.OnComplete (ConcurrentTask.Success (Port.Task.Playing isPaused))) ->
            let
                _ =
                    Debug.log "Resume" isPaused
            in
            ( { model | paused = isPaused }, Cmd.none )

        PortTask (Port.Task.OnComplete response) ->
            let
                _ =
                    Debug.log "response" response
            in
            ( model, Cmd.none )

        PortTask (Port.Task.OnProgress ( tasks, cmd )) ->
            ( { model | tasks = tasks }, Cmd.map PortTask cmd )


view : Model -> Html Main.View.Msg
view model =
    div []
        [ img
            [ src <| VitePluginHelper.asset "/assets/logo.png"
            , style "width" "300px"
            ]
            []
        , helloWorld model.count
        , Html.node "my-component"
            [ model.count
                |> String.fromInt
                |> Html.Attributes.attribute "icon"
            ]
            [ Html.text <| "Component Text" ]
        ]


webview : Model -> Html Msg
webview model =
    Html.map MainView <|
        div
            [ class <|
                String.join " "
                    [ "flex"
                    , "gap-2"
                    , "justify-center"
                    , "items-center"
                    , "flex-1"
                    ]
            ]
            [ button
                [ class "btn"
                , onClick Main.View.PrevTrack
                ]
                [ Icons.skipBackwardIcon ]
            , if model.paused then
                button
                    [ class "btn"
                    , onClick Main.View.Resume
                    ]
                    [ Icons.resumeIcon ]

              else
                button
                    [ class "btn"
                    , onClick Main.View.Pause
                    ]
                    [ Icons.pauseIcon ]
            , button
                [ class "btn"
                , onClick Main.View.NextTrack
                ]
                [ Icons.skipForwardIcon ]
            ]


main : Program () Model Msg
main =
    Browser.element
        { init = init
        , update = update
        , view = webview
        , subscriptions = subscriptions
        }
