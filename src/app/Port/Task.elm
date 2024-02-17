port module Port.Task exposing (..)

import ConcurrentTask exposing (ConcurrentTask)
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


type alias DashboardPayload =
    { todo : String, post : String, album : String }
