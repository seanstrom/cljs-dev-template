port module Port.Hook exposing
    ( Msg
    , Payload(..)
    , empty
    , encode
    , inbox
    , object
    , outbox
    , string
    )

import Json.Encode as JE


port outbox : Msg -> Cmd msg


port inbox : (Msg -> msg) -> Sub msg


type alias Msg =
    JE.Value


type Payload
    = Payload
        { tag : String
        , value : JE.Value
        }


empty : String -> Payload
empty tag =
    Payload
        { tag = tag
        , value = JE.null
        }


string : String -> String -> Payload
string tag s =
    Payload
        { tag = tag
        , value = JE.string s
        }


object : String -> List ( String, JE.Value ) -> Payload
object tag fields =
    Payload
        { tag = tag
        , value = JE.object fields
        }


encode : Payload -> JE.Value
encode (Payload { tag, value }) =
    JE.object
        [ ( "tag", JE.string tag )
        , ( "value", value )
        ]
