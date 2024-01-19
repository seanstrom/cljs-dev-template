module HelloWorld exposing (helloWorld)

import Html exposing (Html, a, button, code, div, h1, p, text)
import Html.Attributes exposing (class, href)
import Html.Events exposing (onClick)
import Msg exposing (Msg(..))


helloWorld : Int -> Html Msg
helloWorld model =
    div []
        [ h1 [class "text-3xl font-bold underline"] [ text "Hello, Vite + Elm!" ]
        , p []
            [ a [ href "https://vitejs.dev/guide/features.html" ] [ text "Vite Documentation" ]
            , text " | "
            , a [ href "https://guide.elm-lang.org/" ] [ text "Elm Documentation" ]
            ]
        , button [ onClick Increment ] [ text "+" ]
        , text <| "Count is: " ++ String.fromInt model
        , button [ onClick Decrement ] [ text "-" ]
        , p []
            [ text "Edit "
            , code [] [ text "src/App.elm" ]
            , text " to test auto refresh"
            ]
        ]
