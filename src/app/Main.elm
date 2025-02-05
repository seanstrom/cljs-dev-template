module Main exposing (main)

import Bridge
import Browser
import Codec
import ConcurrentTask
import Html exposing (Html, button, div, img)
import Html.Attributes exposing (class, src, style)
import Html.Events exposing (onClick)
import Json.Decode as Decode
import Json.Encode as Encode
import Main.View
import Port
import Port.Hook
import Port.Task
import Stuff.HelloWorld exposing (helloWorld)
import Stuff.Icons as Icons
import VitePluginHelper


type Msg
    = PortHook Port.Hook.Msg
    | PortTask Port.Task.Msg
    | MainView Main.View.WebMsg


type alias Model =
    { tasks :
        ConcurrentTask.Pool
            Port.Task.Msg
            Port.Task.Error
            Port.Task.Success
    , paused : Bool
    , count : Int
    , spotifyContext : Maybe Bridge.SpotifyContext
    }


bootSpotify : ConcurrentTask.ConcurrentTask x Port.Task.Success
bootSpotify =
    ConcurrentTask.define
        { function = "boot"
        , expect = ConcurrentTask.expectJson (Codec.decoder Bridge.spotifyContextCodec)
        , errors = ConcurrentTask.expectNoErrors
        , args = Encode.null
        }
        |> ConcurrentTask.map Port.Task.SpotifyContext


type alias PlayPlaylistArgs =
    { spotifyContext : Bridge.SpotifyContext
    , playlist : Bridge.Playlist
    }


playPlaylistArgsCodec =
    Codec.object PlayPlaylistArgs
        |> Codec.field "spotifyContext" .spotifyContext Bridge.spotifyContextCodec
        |> Codec.field "playlist" .playlist Bridge.playlistCodec
        |> Codec.buildObject


playPlaylist : Bridge.SpotifyContext -> Bridge.Playlist -> ConcurrentTask.ConcurrentTask x Port.Task.Success
playPlaylist spotifyContext playlist =
    ConcurrentTask.define
        { function = "playPlaylist"
        , expect = ConcurrentTask.expectWhatever
        , errors = ConcurrentTask.expectNoErrors
        , args =
            Codec.encodeToValue playPlaylistArgsCodec <|
                { spotifyContext = spotifyContext
                , playlist = playlist
                }
        }
        |> ConcurrentTask.map Port.Task.Whatever


resumePlayer : ConcurrentTask.ConcurrentTask x Port.Task.Success
resumePlayer =
    ConcurrentTask.define
        { function = "resumePlayer"
        , expect = ConcurrentTask.expectJson Decode.bool
        , errors = ConcurrentTask.expectNoErrors
        , args = Encode.null
        }
        |> ConcurrentTask.map Port.Task.Playing


sendBackendMsg : Encode.Value -> ConcurrentTask.ConcurrentTask x Bridge.FromBackendMsg
sendBackendMsg args =
    ConcurrentTask.define
        { function = "sendBackendMsg"
        , expect = ConcurrentTask.expectJson Bridge.backendReplyDecoder
        , errors = ConcurrentTask.expectNoErrors
        , args = args
        }


runTask task =
    ConcurrentTask.attempt
        { send = Port.Task.run
        , pool = ConcurrentTask.pool
        , onComplete = Port.Task.OnComplete
        }
        task


watchTasks tasks =
    ConcurrentTask.onProgress
        { send = Port.Task.run
        , receive = Port.Task.track
        , onProgress = Port.Task.OnProgress
        }
        tasks


getPlaylist playlistId =
    Bridge.GetSpotifyPlaylist { playlistId = playlistId }
        |> Bridge.backendMsgEncode
        |> sendBackendMsg
        |> ConcurrentTask.map Port.Task.FromBackend


init : () -> ( Model, Cmd Msg )
init flags =
    let
        playlistId =
            "37i9dQZEVXcD8aCW1Jk6NB"

        autoPlayWithContextAndPlaylist : Bridge.SpotifyContext -> Port.Task.Success -> ConcurrentTask.ConcurrentTask x Port.Task.Success
        autoPlayWithContextAndPlaylist spotifyContext msg =
            case msg of
                Port.Task.FromBackend (Bridge.GotSpotifyPlaylist playlist) ->
                    playPlaylist spotifyContext playlist

                _ ->
                    ConcurrentTask.succeed <| Port.Task.Whatever ()

        autoPlayWithContext msg =
            case msg of
                Port.Task.SpotifyContext spotifyContext ->
                    getPlaylist playlistId
                        |> ConcurrentTask.andThen (autoPlayWithContextAndPlaylist spotifyContext)

                _ ->
                    ConcurrentTask.succeed <| Port.Task.Whatever ()

        ( tasks, cmd ) =
            bootSpotify
                |> ConcurrentTask.andThen autoPlayWithContext
                |> runTask
    in
    ( { tasks = tasks
      , paused = True
      , count = 0
      , spotifyContext = Nothing
      }
    , Cmd.map PortTask cmd
    )


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.batch
        [ Port.Hook.inbox <|
            Port.receive PortHook
        , Sub.map PortTask <|
            watchTasks model.tasks
        ]


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        MainView Main.View.PrevTrack ->
            ( model
            , Port.send <|
                Port.Hook.Payload
                    { tag = "action:previousTrack"
                    , value = Encode.null
                    }
            )

        MainView Main.View.NextTrack ->
            ( model
            , Port.send <|
                Port.Hook.Payload
                    { tag = "action:nextTrack"
                    , value = Encode.null
                    }
            )

        MainView Main.View.Resume ->
            let
                ( tasks, cmd ) =
                    runTask resumePlayer
            in
            ( { model
                | paused = False
                , tasks = tasks
              }
            , Cmd.map PortTask cmd
            )

        MainView Main.View.Pause ->
            ( { model | paused = True }
            , Port.send <|
                Port.Hook.Payload
                    { tag = "action:pause"
                    , value = Encode.null
                    }
            )

        PortHook _ ->
            let
                _ =
                    Debug.log "IncomingMessage" ( model.paused, Cmd.none )
            in
            ( model, Cmd.none )

        PortTask (Port.Task.OnComplete (ConcurrentTask.Success (Port.Task.Playing isPaused))) ->
            let
                _ =
                    Debug.log "Resume" isPaused
            in
            ( { model | paused = isPaused }, Cmd.none )

        PortTask (Port.Task.OnComplete (ConcurrentTask.Success (Port.Task.FromBackend payload))) ->
            let
                _ =
                    Debug.log "payload" payload
            in
            ( model, Cmd.none )

        PortTask (Port.Task.OnComplete (ConcurrentTask.Success (Port.Task.SpotifyContext spotifyContext))) ->
            ( { model | spotifyContext = Just spotifyContext }, Cmd.none )

        PortTask (Port.Task.OnComplete response) ->
            let
                _ =
                    Debug.log "response" response
            in
            ( model, Cmd.none )

        PortTask (Port.Task.OnProgress ( tasks, cmd )) ->
            ( { model | tasks = tasks }, Cmd.map PortTask cmd )


view : Model -> Html Main.View.Msg
view model =
    div []
        [ img
            [ src <| VitePluginHelper.asset "/assets/logo.png"
            , style "width" "300px"
            ]
            []
        , helloWorld model.count
        , Html.node "my-component"
            [ model.count
                |> String.fromInt
                |> Html.Attributes.attribute "icon"
            ]
            [ Html.text <| "Component Text" ]
        ]


webview : Model -> Html Msg
webview model =
    Html.map MainView <|
        div
            [ class <|
                String.join " "
                    [ "flex"
                    , "gap-2"
                    , "justify-center"
                    , "items-center"
                    , "flex-1"
                    ]
            ]
            [ button
                [ class "btn"
                , onClick Main.View.PrevTrack
                ]
                [ Icons.skipBackwardIcon ]
            , if model.paused then
                button
                    [ class "btn"
                    , onClick Main.View.Resume
                    ]
                    [ Icons.resumeIcon ]

              else
                button
                    [ class "btn"
                    , onClick Main.View.Pause
                    ]
                    [ Icons.pauseIcon ]
            , button
                [ class "btn"
                , onClick Main.View.NextTrack
                ]
                [ Icons.skipForwardIcon ]
            ]


main : Program () Model Msg
main =
    Browser.element
        { init = init
        , update = update
        , view = webview
        , subscriptions = subscriptions
        }
