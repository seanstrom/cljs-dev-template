port module Main exposing (main)

import Browser
import HelloWorld exposing (helloWorld)
import Html exposing (Html, div, img)
import Html.Attributes exposing (src, style)
import Json.Encode as Encode
import Message exposing (Message(..))
import Msg exposing (Msg(..))
import VitePluginHelper


type alias Anything =
    Encode.Value


type alias PortMessage =
    Encode.Value


port outbox : PortMessage -> Cmd msg


port inbox : (PortMessage -> msg) -> Sub msg


type alias Model =
    Int


init : () -> ( Model, Cmd Msg )
init flags =
    ( 0, Cmd.none )


receive : PortMessage -> Msg
receive message =
    case message of
        _ ->
            IncomingMessage


send : Message -> Cmd msg
send message =
    outbox <| Message.encode message


subscriptions : Model -> Sub Msg
subscriptions model =
    inbox receive


main : Program () Int Msg
main =
    Browser.element { init = init, update = update, view = view, subscriptions = subscriptions }


update : Msg -> Int -> ( Model, Cmd Msg )
update msg model =
    case msg of
        IncomingMessage ->
            Debug.log "IncomingMessage" ( model + 1, Cmd.none )

        Increment ->
            ( model + 1
            , send <|
                Message
                    { tag = "action:increment"
                    , value = Encode.int model
                    }
            )

        Decrement ->
            ( model - 1, Cmd.none )


view model =
    div []
        [ img
            [ src <| VitePluginHelper.asset "/assets/logo.png"
            , style "width" "300px"
            ]
            []
        , helloWorld model
        , Html.node "my-component"
            [ model
                |> String.fromInt
                |> Html.Attributes.attribute "icon"
            ]
            [ Html.text <| "Component Text" ]
        ]
