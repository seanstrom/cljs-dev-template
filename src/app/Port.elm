port module Port exposing (receive, sendMessage)

import Json.Encode as JE
import Message exposing (Message)


sendMessage : Message -> Cmd msg
sendMessage =
    Message.encode >> send


port send : JE.Value -> Cmd msg


port receive : (JE.Value -> msg) -> Sub msg
