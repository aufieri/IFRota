package br.edu.ifsp.ifrota.poc_fotos

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                FotosScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

@Composable
fun FotosScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var fotos by remember { mutableStateOf(carregarFotos(context)) }

    val launcher = rememberLauncherForActivityResult(PickVisualMedia()) { uri: Uri? ->
        uri?.let {
            salvarFoto(context, it)
            fotos = carregarFotos(context)
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(8.dp)) {
        Button(onClick = { launcher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }) {
            Text("Selecionar Foto")
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(fotos) { foto ->
                AsyncImage(
                    model = foto,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.aspectRatio(1f)
                )
            }
        }
    }
}

fun salvarFoto(context: Context, uri: Uri) {
    val inputStream = context.contentResolver.openInputStream(uri) ?: return
    val arquivo = File(context.filesDir, "${UUID.randomUUID()}.jpg")
    arquivo.outputStream().use { inputStream.copyTo(it) }
    inputStream.close()
}

fun carregarFotos(context: Context): List<File> {
    return context.filesDir.listFiles()
        ?.filter { it.isFile && it.extension == "jpg" }
        ?.sortedByDescending { it.lastModified() }
        ?: emptyList()
}
