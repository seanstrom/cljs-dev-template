module Stuff.Icons exposing (..)

import Svg exposing (path, svg)
import Svg.Attributes as SvgAttr


skipBackwardIcon =
    svg
        [ SvgAttr.fill "none"
        , SvgAttr.stroke "currentColor"
        , SvgAttr.strokeLinecap "round"
        , SvgAttr.strokeLinejoin "round"
        , SvgAttr.strokeWidth "2"
        , SvgAttr.viewBox "0 0 24 24"
        , SvgAttr.height "1em"
        , SvgAttr.width "1em"
        ]
        [ path
            [ SvgAttr.d "M19 20L9 12l10-8v16zM5 19V5"
            ]
            []
        ]


skipForwardIcon =
    svg
        [ SvgAttr.fill "none"
        , SvgAttr.stroke "currentColor"
        , SvgAttr.strokeLinecap "round"
        , SvgAttr.strokeLinejoin "round"
        , SvgAttr.strokeWidth "2"
        , SvgAttr.viewBox "0 0 24 24"
        , SvgAttr.height "1em"
        , SvgAttr.width "1em"
        ]
        [ path
            [ SvgAttr.d "M5 4l10 8-10 8V4zM19 5v14"
            ]
            []
        ]


pauseIcon =
    svg
        [ SvgAttr.fill "none"
        , SvgAttr.stroke "currentColor"
        , SvgAttr.strokeLinecap "round"
        , SvgAttr.strokeLinejoin "round"
        , SvgAttr.strokeWidth "2"
        , SvgAttr.viewBox "0 0 24 24"
        , SvgAttr.height "1em"
        , SvgAttr.width "1em"
        ]
        [ path
            [ SvgAttr.d "M6 4h4v16H6zM14 4h4v16h-4z"
            ]
            []
        ]


resumeIcon =
    svg
        [ SvgAttr.fill "none"
        , SvgAttr.stroke "currentColor"
        , SvgAttr.strokeLinecap "round"
        , SvgAttr.strokeLinejoin "round"
        , SvgAttr.strokeWidth "2"
        , SvgAttr.viewBox "0 0 24 24"
        , SvgAttr.height "1em"
        , SvgAttr.width "1em"
        ]
        [ path
            [ SvgAttr.d "M5 3l14 9-14 9V3z"
            ]
            []
        ]
