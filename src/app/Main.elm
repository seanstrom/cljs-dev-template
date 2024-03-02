module Main exposing (main)

import Browser
import ConcurrentTask
import Html exposing (Html, div, img)
import Html.Attributes exposing (src, style)
import Json.Encode as Encode
import Main.View
import Port
import Port.Hook
import Port.Item
import Port.Task
import Stuff.HelloWorld exposing (helloWorld)
import VitePluginHelper


type Msg
    = PortHook Port.Hook.Msg
    | PortTask Port.Task.Msg
    | MainView Main.View.Msg


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
                Port.Item.getAllTitles
    in
    ( { tasks = tasks
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
        MainView Main.View.Increment ->
            ( { model | count = model.count + 1 }
            , Port.send <|
                Port.Hook.Payload
                    { tag = "action:increment"
                    , value = Encode.int model.count
                    }
            )

        MainView Main.View.Decrement ->
            ( { model | count = model.count - 1 }, Cmd.none )

        PortHook _ ->
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
    div []
        [ img
            [ src <| VitePluginHelper.asset "/assets/logo.png"
            , style "width" "300px"
            ]
            []
        , Html.map MainView <| helloWorld model.count
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
