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
                onStartDownload = { url, fmt, playlist, subs, sb, archive, subLangs ->
                    val enc = URLEncoder.encode(url, "UTF-8")
                    val fmtEnc = URLEncoder.encode(fmt, "UTF-8")
                    navController.navigate("progress/$enc/$fmtEnc/$playlist/$subs/$sb/$archive/$subLangs")
                }
            )
        }
        composable("progress/{url}/{fmt}/{playlist}/{subs}/{sb}/{archive}/{subLangs}") { back ->
            val args = back.arguments!!
            ProgressScreen(
                url        = URLDecoder.decode(args.getString("url") ?: "", "UTF-8"),
                fmt        = URLDecoder.decode(args.getString("fmt") ?: "bv*+ba/b", "UTF-8"),
                playlist   = args.getString("playlist")?.toBoolean() ?: false,
                subs       = args.getString("subs")?.toBoolean() ?: false,
                sponsorblock = args.getString("sb")?.toBoolean() ?: false,
                archive    = args.getString("archive")?.toBoolean() ?: false,
                subLangs   = args.getString("subLangs") ?: "en,ar",
                onBack     = { navController.popBackStack() }
            )
        }
    }
}
