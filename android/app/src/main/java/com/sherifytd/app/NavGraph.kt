package com.sherifytd.app

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sherifytd.app.ui.screens.HomeScreen
import com.sherifytd.app.ui.screens.ProgressScreen
import java.net.URLDecoder
import java.net.URLEncoder

@Composable
fun NavGraph(sharedUrl: String? = null) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                sharedUrl = sharedUrl,
                onStartDownload = { url, fmtIndex, playlist, subs, sb, archive, subLangs ->
                    val enc = URLEncoder.encode(url, "UTF-8")
                    val langsEnc = URLEncoder.encode(subLangs, "UTF-8")
                    navController.navigate("progress/$enc/$fmtIndex/$playlist/$subs/$sb/$archive/$langsEnc")
                }
            )
        }
        composable("progress/{url}/{fmtIndex}/{playlist}/{subs}/{sb}/{archive}/{subLangs}") { back ->
            val args = back.arguments!!
            val fmtIndex = args.getString("fmtIndex")?.toIntOrNull() ?: 0
            ProgressScreen(
                url          = URLDecoder.decode(args.getString("url") ?: "", "UTF-8"),
                fmt          = FORMATS.getOrElse(fmtIndex) { FORMATS[0] }.value,
                playlist     = args.getString("playlist")?.toBoolean() ?: false,
                subs         = args.getString("subs")?.toBoolean() ?: false,
                sponsorblock = args.getString("sb")?.toBoolean() ?: false,
                archive      = args.getString("archive")?.toBoolean() ?: false,
                subLangs     = URLDecoder.decode(args.getString("subLangs") ?: "en,ar", "UTF-8"),
                onBack       = { navController.popBackStack() }
            )
        }
    }
}
