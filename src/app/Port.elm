module Port exposing (..)

import Port.Message


receive : (Port.Message.Msg -> msg) -> Port.Message.Msg -> msg
receive toMsg message =
    case message of
        _ ->
            toMsg message


send : Port.Message.Payload -> Cmd msg
send payload =
    Port.Message.outbox <| Port.Message.encode payload
