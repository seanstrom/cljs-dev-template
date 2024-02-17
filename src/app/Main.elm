port module Main exposing (main)

import Browser
import ConcurrentTask exposing (ConcurrentTask)
import ConcurrentTask.Http as Http
import HelloWorld exposing (helloWorld)
import Html exposing (Html, div, img)
import Html.Attributes exposing (src, style)
import Json.Decode as Decode
import Json.Encode as Encode
import Message exposing (Message(..))
import Msg as ViewMsg
import VitePluginHelper


type alias Anything =
    Encode.Value


type alias PortMessage =
    Encode.Value


port outbox : PortMessage -> Cmd msg


port inbox : (PortMessage -> msg) -> Sub msg


type PortTaskMsg
    = PortTaskProgress
        ( ConcurrentTask.Pool
            PortTaskMsg
            PortTaskError
            PortTaskSuccess
        , Cmd PortTaskMsg
        )
    | PortTaskComplete (ConcurrentTask.Response PortTaskError PortTaskSuccess)


type alias PortTaskError =
    Http.Error


type alias DashboardPayload =
    { todo : String, post : String, album : String }


type PortTaskSuccess
    = Dashboard DashboardPayload


type Msg
    = PortTask PortTaskMsg
    | PortMsg PortMessage
    | ViewMsg ViewMsg.Msg


toDashboard : String -> String -> String -> PortTaskSuccess
toDashboard todos posts albums =
    DashboardPayload todos posts albums |> Dashboard


getTitle : String -> ConcurrentTask PortTaskError String
getTitle path =
    Http.get
        { url = "https://jsonplaceholder.typicode.com" ++ path
        , headers = []
        , expect = Http.expectJson (Decode.field "title" Decode.string)
        , timeout = Nothing
        }


getAllTitles : ConcurrentTask PortTaskError PortTaskSuccess
getAllTitles =
    ConcurrentTask.succeed toDashboard
        |> ConcurrentTask.andMap (getTitle "/todos/1")
        |> ConcurrentTask.andMap (getTitle "/posts/1")
        |> ConcurrentTask.andMap (getTitle "/albums/1")


type alias Model =
    { tasks : ConcurrentTask.Pool PortTaskMsg PortTaskError PortTaskSuccess
    , count : Int
    }


port sendPortTask : Decode.Value -> Cmd msg


port receivePortTask : (Decode.Value -> msg) -> Sub msg


init : () -> ( Model, Cmd Msg )
init flags =
    let
        ( tasks, cmd ) =
            ConcurrentTask.attempt
                { send = sendPortTask
                , pool = ConcurrentTask.pool
                , onComplete = PortTaskComplete
                }
                getAllTitles
    in
    ( { tasks = tasks, count = 0 }, Cmd.map PortTask cmd )


receive : PortMessage -> Msg
receive message =
    case message of
        _ ->
            PortMsg message


send : Message -> Cmd msg
send message =
    outbox <| Message.encode message


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.batch
        [ inbox receive
        , ConcurrentTask.onProgress
            { send = sendPortTask
            , receive = receivePortTask
            , onProgress = PortTaskProgress
            }
            model.tasks
            |> Sub.map PortTask
        ]


mainView model =
    Html.map ViewMsg (view model)


main : Program () Model Msg
main =
    Browser.element { init = init, update = update, view = mainView, subscriptions = subscriptions }


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        ViewMsg ViewMsg.Increment ->
            ( { model | count = model.count + 1 }
            , send <|
                Message
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

        PortTask (PortTaskComplete response) ->
            let
                _ =
                    Debug.log "response" response
            in
            ( model, Cmd.none )

        PortTask (PortTaskProgress ( tasks, cmd )) ->
            ( { model | tasks = tasks }, Cmd.map PortTask cmd )


view : Model -> Html ViewMsg.Msg
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
