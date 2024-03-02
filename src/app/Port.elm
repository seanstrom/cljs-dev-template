module Port exposing (..)

import Port.Hook


receive : (Port.Hook.Msg -> msg) -> Port.Hook.Msg -> msg
receive toMsg message =
    case message of
        _ ->
            toMsg message


send : Port.Hook.Payload -> Cmd msg
send payload =
    Port.Hook.outbox <| Port.Hook.encode payload
