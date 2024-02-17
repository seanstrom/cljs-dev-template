module Main exposing (main)

import Browser
import ConcurrentTask exposing (ConcurrentTask)
import HelloWorld exposing (helloWorld)
import Html exposing (Html, div, img)
import Html.Attributes exposing (src, style)
import Json.Encode as Encode
import Msg as ViewMsg
import Port
import Port.Entity
import Port.Message
import Port.Task
import VitePluginHelper


type Msg
    = PortMsg Port.Message.Msg
    | PortTask Port.Task.Msg
    | ViewMsg ViewMsg.Msg


type alias Model =
    { tasks :
        ConcurrentTask.Pool
            Port.Task.Msg
            Port.Task.Error
            Port.Task.Success
    , count : Int
    }


init : () -> ( Model, Cmd Msg )
init flags =
    let
        ( tasks, cmd ) =
            ConcurrentTask.attempt
                { send = Port.Task.run
                , pool = ConcurrentTask.pool
                , onComplete = Port.Task.OnComplete
                }
                Port.Entity.getAllTitles
    in
    ( { tasks = tasks
      , count = 0
      }
    , Cmd.map PortTask cmd
    )


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.batch
        [ Port.Message.inbox <|
            Port.receive PortMsg
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
        ViewMsg ViewMsg.Increment ->
            ( { model | count = model.count + 1 }
            , Port.send <|
                Port.Message.Payload
                    { tag = "action:increment"
                    , value = Encode.int model.count
                    }
            )

        ViewMsg ViewMsg.Decrement ->
            ( { model | count = model.count - 1 }, Cmd.none )

        PortMsg _ ->
            let
                _ =
                    Debug.log "IncomingMessage" ( model.count + 1, Cmd.none )
            in
            ( model, Cmd.none )

        PortTask (Port.Task.OnComplete response) ->
            let
                _ =
                    Debug.log "response" response
            in
            ( model, Cmd.none )

        PortTask (Port.Task.OnProgress ( tasks, cmd )) ->
            ( { model | tasks = tasks }, Cmd.map PortTask cmd )


view : Model -> Html Msg
view model =
    Html.map ViewMsg <|
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


main : Program () Model Msg
main =
    Browser.element
        { init = init
        , update = update
        , view = view
        , subscriptions = subscriptions
        }
