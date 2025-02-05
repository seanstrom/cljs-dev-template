module Bridge exposing (..)

import Codec
import Json.Decode as Decode
import Json.Encode as Encode


type alias PlaylistRequestInfo =
    { playlistId : String }


type alias PlaylistTrack =
    { uid : String
    , uri : String
    , title : String
    , subtitle : String
    }


type alias Playlist =
    { id : String
    , uri : String
    , name : String
    , trackList : List PlaylistTrack
    }


type ToBackendMsg
    = GetSpotifyPlaylist PlaylistRequestInfo


type FromBackendMsg
    = GotSpotifyPlaylist Playlist


type alias CodecHelper msg a =
    { tag : String
    , tagFieldName : String
    , valueFieldName : String
    , toMsg : a -> msg
    , decoder : Decode.Decoder a
    , encode : a -> Encode.Value
    }


type alias SpotifyContext =
    { spotify : Decode.Value
    , accessToken : String
    , deviceId : String
    }


spotifyContextCodec =
    Codec.object SpotifyContext
        |> Codec.field "spotify" .spotify Codec.value
        |> Codec.field "accessToken" .accessToken Codec.string
        |> Codec.field "deviceId" .deviceId Codec.string
        |> Codec.buildObject


playlistRequestInfoCodec =
    Codec.object PlaylistRequestInfo
        |> Codec.field "playlistId" .playlistId Codec.string
        |> Codec.buildObject


playlistTrackCodec =
    Codec.object PlaylistTrack
        |> Codec.field "uid" .uid Codec.string
        |> Codec.field "uri" .uri Codec.string
        |> Codec.field "title" .title Codec.string
        |> Codec.field "subtitle" .subtitle Codec.string
        |> Codec.buildObject


playlistCodec =
    Codec.object Playlist
        |> Codec.field "id" .id Codec.string
        |> Codec.field "uri" .uri Codec.string
        |> Codec.field "name" .name Codec.string
        |> Codec.field "trackList" .trackList (Codec.list playlistTrackCodec)
        |> Codec.buildObject


getSpotifyPlaylist : CodecHelper ToBackendMsg PlaylistRequestInfo
getSpotifyPlaylist =
    let
        toMsg =
            GetSpotifyPlaylist

        tag =
            "GetSpotifyPlaylist"

        tagFieldName =
            "tag"

        valueFieldName =
            "value"

        detailsCodec =
            playlistRequestInfoCodec

        encode msg =
            Encode.object
                [ ( tagFieldName, Encode.string tag )
                , ( valueFieldName, Codec.encodeToValue detailsCodec msg )
                ]

        decoder =
            Decode.field valueFieldName (Codec.decoder detailsCodec)
    in
    { tag = tag
    , tagFieldName = tagFieldName
    , valueFieldName = valueFieldName
    , toMsg = toMsg
    , decoder = decoder
    , encode = encode
    }


gotSpotifyPlaylist : CodecHelper FromBackendMsg Playlist
gotSpotifyPlaylist =
    let
        toMsg =
            GotSpotifyPlaylist

        tag =
            "GotSpotifyPlaylist"

        tagFieldName =
            "tag"

        valueFieldName =
            "value"

        detailsCodec =
            playlistCodec

        encode msg =
            Encode.object
                [ ( tagFieldName, Encode.string tag )
                , ( valueFieldName, Codec.encodeToValue detailsCodec msg )
                ]

        decoder =
            Decode.field valueFieldName (Codec.decoder detailsCodec)
    in
    { tag = tag
    , tagFieldName = tagFieldName
    , valueFieldName = valueFieldName
    , toMsg = toMsg
    , decoder = decoder
    , encode = encode
    }


backendMsgEncode : ToBackendMsg -> Encode.Value
backendMsgEncode msg =
    case msg of
        GetSpotifyPlaylist info ->
            getSpotifyPlaylist.encode info


makeBackendMsgDecoder : CodecHelper ToBackendMsg msg -> Decode.Decoder ToBackendMsg
makeBackendMsgDecoder codecHelper =
    codecHelper.decoder |> Decode.map codecHelper.toMsg


backendMsgDecoder : Decode.Decoder ToBackendMsg
backendMsgDecoder =
    [ getSpotifyPlaylist ]
        |> List.map makeBackendMsgDecoder
        |> Decode.oneOf


backendMsgCodec : Codec.Codec ToBackendMsg
backendMsgCodec =
    Codec.build backendMsgEncode backendMsgDecoder


backendReplyEncode : FromBackendMsg -> Encode.Value
backendReplyEncode msg =
    case msg of
        GotSpotifyPlaylist playlist ->
            gotSpotifyPlaylist.encode playlist


makeBackendReplyDecoder : CodecHelper FromBackendMsg msg -> Decode.Decoder FromBackendMsg
makeBackendReplyDecoder codecHelper =
    codecHelper.decoder |> Decode.map codecHelper.toMsg


backendReplyDecoder : Decode.Decoder FromBackendMsg
backendReplyDecoder =
    [ gotSpotifyPlaylist ]
        |> List.map makeBackendReplyDecoder
        |> Decode.oneOf


backendReplyCodec : Codec.Codec FromBackendMsg
backendReplyCodec =
    Codec.build backendReplyEncode backendReplyDecoder
