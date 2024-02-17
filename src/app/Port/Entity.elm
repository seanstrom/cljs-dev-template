module Port.Entity exposing (..)

import ConcurrentTask exposing (ConcurrentTask)
import ConcurrentTask.Http as Http
import Json.Decode as Decode
import Port.Task


toDashboard : String -> String -> String -> Port.Task.Success
toDashboard todos posts albums =
    Port.Task.DashboardPayload todos posts albums |> Port.Task.Dashboard


getTitle : String -> ConcurrentTask Port.Task.Error String
getTitle path =
    Http.get
        { url = "https://jsonplaceholder.typicode.com" ++ path
        , headers = []
        , expect = Http.expectJson (Decode.field "title" Decode.string)
        , timeout = Nothing
        }


getAllTitles : ConcurrentTask Port.Task.Error Port.Task.Success
getAllTitles =
    ConcurrentTask.succeed toDashboard
        |> ConcurrentTask.andMap (getTitle "/todos/1")
        |> ConcurrentTask.andMap (getTitle "/posts/1")
        |> ConcurrentTask.andMap (getTitle "/albums/1")
