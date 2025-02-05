port module Port.Task exposing (..)

import Bridge
import ConcurrentTask
import ConcurrentTask.Http as Http
import Json.Encode as JE


port run : JE.Value -> Cmd msg


port track : (JE.Value -> msg) -> Sub msg


type Msg
    = OnProgress
        ( ConcurrentTask.Pool
            Msg
            Error
            Success
        , Cmd Msg
        )
    | OnComplete (ConcurrentTask.Response Error Success)


type alias Error =
    Http.Error


type Success
    = Dashboard DashboardPayload
    | Whatever ()
    | Playing Bool
    | FromBackend Bridge.FromBackendMsg
    | SpotifyEmbedHtml String
    | SpotifyContext Bridge.SpotifyContext


type alias DashboardPayload =
    { todo : String, post : String, album : String }
