module Message exposing (Message(..), empty, encode, object, string)

import Json.Encode as JE


type Message
    = Message
        { tag : String
        , value : JE.Value
        }


empty : String -> Message
empty tag =
    Message
        { tag = tag
        , value = JE.null
        }


string : String -> String -> Message
string tag s =
    Message
        { tag = tag
        , value = JE.string s
        }


object : String -> List ( String, JE.Value ) -> Message
object tag fields =
    Message
        { tag = tag
        , value = JE.object fields
        }


encode : Message -> JE.Value
encode (Message { tag, value }) =
    JE.object
        [ ( "tag", JE.string tag )
        , ( "value", value )
        ]
