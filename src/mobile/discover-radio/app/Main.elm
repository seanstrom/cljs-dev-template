module Main exposing (..)

import Browser
import Html exposing (..)
import Html.Attributes exposing (..)
import Html.Events exposing (..)
import Json.Decode as Decode
import String



-- Main


main : Program () Model Msg
main =
    Browser.element
        { init = init
        , update = update
        , subscriptions = subscriptions
        , view = view
        }



-- Model


type alias Model =
    Int



-- Init


init : () -> ( Model, Cmd Msg )
init _ =
    ( 0, Cmd.none )



-- UPDATE


type Msg
    = Increment
    | Decrement


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        Increment ->
            ( model + 1, Cmd.none )

        Decrement ->
            ( model - 1, Cmd.none )



-- Subscriptions


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.none



-- Events


onTap : msg -> Attribute msg
onTap msg =
    on "tap" (Decode.succeed msg)



-- View


view : Model -> Html Msg
view model =
    node "Page"
        []
        [ -- node "ActionBar" [ attribute "title" "Home" ] [],
          node "StackLayout"
            []
            [ node "Button" [ attribute "text" "Increment", onTap Increment ] []
            , node "Button" [ attribute "text" "Decrement", onTap Decrement ] []
            , node "Label" [ attribute "text" (String.fromInt model) ] []
            ]
        ]
