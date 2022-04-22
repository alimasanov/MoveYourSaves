// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.material.MaterialTheme
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import netscape.javascript.JSObject
import java.awt.BorderLayout
import java.net.URL
import javax.swing.JPanel

@Composable
@Preview
fun App(window: ComposeWindow) {
    var text by remember { mutableStateOf("Войти") }
    val vk by remember { mutableStateOf(VkAuth()) }
    var webView by remember { mutableStateOf(false) }
    var photoList by remember { mutableStateOf(false) }

    MaterialTheme {
        if (webView) {
            webView("https://oauth.vk.com/authorize?client_id=${Constants.VK_APP_ID}" +
                    "&display=page" +
                    "&redirect_uri=${Constants.REDIRECT_URL}" +
                    "&response_type=token" +
                    "&scope=photos" +
                    "&v=5.131",
                window,
                onClose = {
                    vk.userId = URL(it).ref.split("&").map {
                        it.substring(it.indexOf("=") + 1, it.length)
                    }[2].toInt()
                    vk.token = URL(it).ref.split("&").map {
                        it.substring(it.indexOf("=") + 1, it.length)
                    }[0]
                    webView = false
                    photoList = true
                })
        }
        if (photoList) {
            vk.oAuth()
            LazyColumn {
                items(vk.getPhotos().items) {
                    Text(text = it.photo256.toString(),
                    modifier = Modifier)
                }
            }
        }
        Button(onClick = {
            webView = true
        }) {
            Text(text)
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "MoveYourSaves") {
        App(this.window)
    }
}

@Composable
fun webView(url: String, window: ComposeWindow, onClose: (String) -> Unit) {
    val jfxPanel = remember { JFXPanel() }
    var jsObject = remember<JSObject?> { null }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {

        ComposeJFXPanel(
            composeWindow = window,
            jfxPanel = jfxPanel,
            onCreate = {
                Platform.runLater {
                    val root = WebView()
                    val engine = root.engine
                    val scene = Scene(root)
                    engine.loadWorker.stateProperty().addListener { _, _, newState ->
                        if (newState === Worker.State.SUCCEEDED) {
                            jsObject = root.engine.executeScript("window") as JSObject
                            // execute other javascript / setup js callbacks fields etc..
                        }
                    }
                    engine.loadWorker.exceptionProperty().addListener { _, _, newError ->
                        println("page load error : $newError")
                    }
                    engine.locationProperty().addListener { _, _, c ->
                        if (c.startsWith(Constants.REDIRECT_URL)) {
                            onClose.invoke(c)
                        }
                    }
                    jfxPanel.scene = scene
                    engine.load(url) // can be a html document from resources ..
                    engine.setOnError { error -> println("onError : $error") }

                }
            }, onDestroy = {
                Platform.runLater {
                    jsObject?.let { jsObj ->
                        // clean up code for more complex implementations i.e. removing javascript callbacks etc..
                    }
                }
            })
    }
}

@Composable
fun ComposeJFXPanel(
    composeWindow: ComposeWindow,
    jfxPanel: JFXPanel,
    onCreate: () -> Unit,
    onDestroy: () -> Unit = {}
) {
    val jPanel = remember { JPanel() }
    val density = LocalDensity.current.density

    Layout(
        content = {},
        modifier = Modifier.onGloballyPositioned { childCoordinates ->
            val coordinates = childCoordinates.parentCoordinates!!
            val location = coordinates.localToWindow(Offset.Zero).round()
            val size = coordinates.size
            jPanel.setBounds(
                (location.x / density).toInt(),
                (location.y / density).toInt(),
                (size.width / density).toInt(),
                (size.height / density).toInt()
            )
            jPanel.validate()
            jPanel.repaint()
        },
        measurePolicy = { _, _ -> layout(0, 0) {} })

    DisposableEffect(jPanel) {
        composeWindow.add(jPanel)
        jPanel.layout = BorderLayout(0, 0)
        jPanel.add(jfxPanel)
        onCreate()
        onDispose {
            onDestroy()
            composeWindow.remove(jPanel)
        }
    }
}
