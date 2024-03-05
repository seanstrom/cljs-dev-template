module Main.View exposing (..)


type Msg
    = Increment
    | Decrement


type WebMsg
    = PrevTrack
    | NextTrack
    | Pause
    | Resume
