module Server exposing (main)

import Bridge
import Codec
import ConcurrentTask
import Json.Decode as Decode
import Json.Encode as Encode
import Platform
import Port
import Port.Hook
import Port.Task


type alias Model =
    { count : Int
    , tasks :
        ConcurrentTask.Pool
            Port.Task.Msg
            Port.Task.Error
            Port.Task.Success
    }


type Msg
    = PortHook Port.Hook.Msg
    | PortTask Port.Task.Msg


init : () -> ( Model, Cmd Msg )
init flags =
    let
        _ =
            Debug.log "init" 5
    in
    ( { tasks = ConcurrentTask.pool
      , count = 0
      }
    , Port.Hook.outbox <| Port.Hook.encode <| Port.Hook.empty "increment"
    )


type alias PortRequest =
    { requestId : String
    , content : Bridge.ToBackendMsg
    }


portRequestCodec : Codec.Codec PortRequest
portRequestCodec =
    Codec.object PortRequest
        |> Codec.field "requestId" .requestId Codec.string
        |> Codec.field "content" .content Bridge.backendMsgCodec
        |> Codec.buildObject



-- spotifyEmbed : String -> ConcurrentTask.ConcurrentTask x String
-- spotifyEmbed playlistId =
--     ConcurrentTask.define
--         { function = "spotifyEmbed"
--         , expect = ConcurrentTask.expectJson Decode.string
--         , errors = ConcurrentTask.expectNoErrors
--         , args = Encode.string playlistId
--         }


spotifyEmbed : String -> ConcurrentTask.ConcurrentTask x Bridge.Playlist
spotifyEmbed playlistId =
    ConcurrentTask.define
        { function = "spotifyEmbed"
        , expect = ConcurrentTask.expectJson (Codec.decoder Bridge.playlistCodec)
        , errors = ConcurrentTask.expectNoErrors
        , args = Encode.string playlistId
        }


respond : String -> Encode.Value -> ConcurrentTask.ConcurrentTask x ()
respond requestId content =
    ConcurrentTask.define
        { function = "respond"
        , expect = ConcurrentTask.expectWhatever
        , errors = ConcurrentTask.expectNoErrors
        , args =
            Encode.object
                [ ( "tag", Encode.string "response" )
                , ( "requestId", Encode.string requestId )
                , ( "content", content )
                ]
        }


runTask task =
    ConcurrentTask.attempt
        { send = Port.Task.run
        , pool = ConcurrentTask.pool
        , onComplete = Port.Task.OnComplete
        }
        task


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        PortHook portHookMsg ->
            let
                requestDecoder =
                    Decode.oneOf
                        [ Codec.decoder portRequestCodec
                        ]

                decodedRequest =
                    Decode.decodeValue requestDecoder portHookMsg

                respondTo request payload =
                    payload
                        |> Bridge.gotSpotifyPlaylist.toMsg
                        |> Bridge.backendReplyEncode
                        |> respond request.requestId
            in
            case decodedRequest of
                Ok request ->
                    case request.content of
                        Bridge.GetSpotifyPlaylist playlist ->
                            let
                                ( tasks, cmd ) =
                                    spotifyEmbed playlist.playlistId
                                        |> ConcurrentTask.andThen (respondTo request)
                                        |> ConcurrentTask.map Port.Task.Whatever
                                        |> runTask
                            in
                            ( { model | tasks = tasks }
                            , Cmd.map PortTask cmd
                            )

                Err _ ->
                    ( model, Cmd.none )

        PortTask (Port.Task.OnComplete response) ->
            let
                _ =
                    Debug.log "response" response
            in
            ( model, Cmd.none )

        PortTask (Port.Task.OnProgress ( tasks, cmd )) ->
            ( { model | tasks = tasks }, Cmd.map PortTask cmd )


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.batch
        [ Port.Hook.inbox <|
            Port.receive PortHook
        , Sub.map PortTask <|
            ConcurrentTask.onProgress
                { send = Port.Task.run
                , receive = Port.Task.track
                , onProgress = Port.Task.OnProgress
                }
                model.tasks
        ]


main =
    Platform.worker
        { init = init
        , update = update
        , subscriptions = subscriptions
        }
