package com.example.imb

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import android.content.Context

import com.example.imb.ui.theme.IMBTheme
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// --- COLORES TÁCTICOS (MARINES STYLE) ---
val MarinesRed = Color(0xFFC8102E)
val MarinesBlack = Color(0xFF0A0A0A)
val MarinesDarkGray = Color(0xFF1A1A1A)
val MarinesWhite = Color.White

// --- DATA MODELS ---
data class NewsItem(
    val id: String = "",
    val title: String = "",
    val date: String = "",
    val content: String = "",
    val category: String = "Infantería",
    val imageUrl: String = "",
    val iconEmoji: String = "⚓"
)

data class UnitSection(
    val category: String,
    val units: List<String>
)

data class MagazineItem(
    val id: String = "",
    val title: String = "",
    val date: String = "",
    val coverUrl: String = "",
    val fileUrl: String = ""
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IMBTheme {
                AppNavigator()
            }
        }
    }
}

@Composable
fun AppNavigator() {
    var currentScreen by remember { mutableStateOf("home") }
    var selectedNewsItem by remember { mutableStateOf<NewsItem?>(null) }
    
    val newsList = remember { mutableStateListOf<NewsItem>() }
    val magazineList = remember { mutableStateListOf<MagazineItem>() }
    val isNewsLoading = remember { mutableStateOf(true) }
    val isMagLoading = remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val database = FirebaseDatabase.getInstance()
            
            database.getReference("news").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val temp = mutableListOf<NewsItem>()
                    for (post in snapshot.children) {
                        try {
                            post.getValue(NewsItem::class.java)?.let { temp.add(it.copy(id = post.key ?: "")) }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                    newsList.clear()
                    newsList.addAll(temp.reversed())
                    isNewsLoading.value = false
                }
                override fun onCancelled(error: DatabaseError) { isNewsLoading.value = false }
            })

            database.getReference("magazines").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val temp = mutableListOf<MagazineItem>()
                    for (post in snapshot.children) {
                        try {
                            val mag = MagazineItem(
                                id = post.key ?: "",
                                title = post.child("title").value?.toString() ?: "",
                                date = post.child("date").value?.toString() ?: "",
                                coverUrl = (post.child("coverUrl").value ?: post.child("cover_url").value)?.toString() ?: "",
                                fileUrl = (post.child("fileUrl").value ?: post.child("file_url").value)?.toString() ?: ""
                            )
                            temp.add(mag)
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                    magazineList.clear()
                    magazineList.addAll(temp.reversed())
                    isMagLoading.value = false
                }
                override fun onCancelled(error: DatabaseError) { isMagLoading.value = false }
            })
        } catch (e: Exception) {
            isNewsLoading.value = false
            isMagLoading.value = false
        }
    }

    val bottomNavScreens = listOf("home", "news_list", "galeria", "revista")

    Scaffold(
        containerColor = MarinesBlack,
        bottomBar = {
            if (currentScreen in bottomNavScreens) {
                NavigationBar(
                    containerColor = MarinesBlack,
                    contentColor = MarinesWhite,
                    tonalElevation = 8.dp
                ) {
                    val navItems = listOf(
                        Triple("home", "Inicio", Icons.Default.Home),
                        Triple("news_list", "Gaceta", Icons.Default.Newspaper),
                        Triple("galeria", "Galería", Icons.Default.PhotoLibrary),
                        Triple("revista", "Revista", Icons.Default.MenuBook)
                    )
                    navItems.forEach { (route, _, icon) ->
                        NavigationBarItem(
                            selected = currentScreen == route,
                            onClick = { currentScreen = route },
                            icon = { 
                                Icon(
                                    imageVector = icon, 
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp) 
                                ) 
                            },
                            label = null,
                            alwaysShowLabel = false,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MarinesWhite,
                                unselectedIconColor = Color.Gray,
                                indicatorColor = MarinesRed
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            Crossfade(targetState = currentScreen, label = "Navigation") { screen ->
                when (screen) {
                    "home" -> AppleStyleHome(
                        newsList = newsList,
                        onNavigateToNews = { currentScreen = "news_list" },
                        onNewsClick = { news ->
                            selectedNewsItem = news
                            currentScreen = "news_detail"
                        },
                        onMenuItemClick = { route -> currentScreen = route }
                    )
                    "news_list" -> DigitalNewspaperScreen(
                        newsItems = newsList,
                        isLoading = isNewsLoading.value,
                        onBack = { currentScreen = "home" },
                        onNewsClick = { news ->
                            selectedNewsItem = news
                            currentScreen = "news_detail"
                        }
                    )
                    "news_detail" -> AppleNewsDetail(
                        news = selectedNewsItem,
                        onBack = { currentScreen = "news_list" }
                    )
                    "codigo_honor" -> HonorCodeScreen(onBack = { currentScreen = "home" })
                    "galeria" -> GalleryScreen(onBack = { currentScreen = "home" })
                    "revista" -> MagazineShelfScreen(
                        magazines = magazineList,
                        isLoading = isMagLoading.value,
                        onBack = { currentScreen = "home" }
                    )
                }
            }
        }
    }
}

// --- PANTALLA: HOME ---
@Composable
fun AppleStyleHome(
    newsList: List<NewsItem>,
    onNavigateToNews: () -> Unit,
    onNewsClick: (NewsItem) -> Unit,
    onMenuItemClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MarinesBlack)
            .verticalScroll(rememberScrollState())
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(550.dp)) {
            Image(
                painter = painterResource(id = R.drawable.banner),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(0.4f), Color.Transparent, MarinesBlack)))
            )

            Column(modifier = Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                Text(
                    text = "INFANTERÍA DE MARINA BOLIVARIANA",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 14.sp,
                        letterSpacing = 1.sp
                    ),
                    color = MarinesWhite,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "FORJANDO EL FUTURO\nCON VALOR Y LEALTAD",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        lineHeight = 42.sp,
                        fontSize = 38.sp
                    ),
                    fontWeight = FontWeight.Black,
                    color = MarinesWhite
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { onMenuItemClick("codigo_honor") },
                    colors = ButtonDefaults.buttonColors(containerColor = MarinesRed),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text("CÓDIGO DE HONOR", color = MarinesWhite, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // --- SECCIÓN DE VALORES (ESTILO MARINES.COM) ---
        Text(
            "VALORES DEFINIDOS POR NUESTRA FORMA DE VIDA",
            modifier = Modifier.padding(horizontal = 24.dp),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
            color = MarinesWhite
        )
        Spacer(modifier = Modifier.height(32.dp))

        ValueRow(
            title = "HONOR",
            description = "El honor guía a los Infantes de Marina para ejemplificar lo último en comportamiento ético y moral. No mentir, no engañar ni robar; cumplir con un código de integridad inquebrantable; respetar la dignidad humana.",
            resId = R.drawable.honor,
            isImageLeft = true
        )

        ValueRow(
            title = "VALOR",
            description = "El valor es la fuerza mental, moral y física arraigada en nosotros. Nos lleva a través de los desafíos del combate y nos ayuda a superar el miedo. Es la fuerza interior que nos permite hacer lo correcto.",
            resId = R.drawable.valor,
            isImageLeft = false
        )

        ValueRow(
            title = "LEALTAD",
            description = "La lealtad es el espíritu de determinación y dedicación. Conduce al más alto orden de disciplina para individuos y unidades. Inspira la determinación incansable de lograr la victoria en cada esfuerzo.",
            resId = R.drawable.lealtad,
            isImageLeft = true
        )

        Spacer(modifier = Modifier.height(32.dp))
        Text("EXPLORAR LA FUERZA", modifier = Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.labelLarge, color = Color.Gray, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (newsList.isNotEmpty()) {
            SectionHeader("NOTICIAS RECIENTES", onNavigateToNews, isDark = true)
            LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(newsList.take(5)) { news -> AppleFeaturedCard(news, onClick = { onNewsClick(news) }) }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        ExploreCard(
            title = "GACETA IMB",
            subtitle = "Últimas noticias y operaciones",
            imageUrl = if (newsList.isNotEmpty()) newsList[0].imageUrl else "https://images.unsplash.com/photo-1579353977828-2a4eab540b9a",
            onClick = onNavigateToNews
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun ValueRow(title: String, description: String, resId: Int, isImageLeft: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isImageLeft) {
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                modifier = Modifier
                    .weight(1.1f)
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f).padding(end = 24.dp)) {
                Box(modifier = Modifier.width(30.dp).height(4.dp).background(MarinesRed))
                Spacer(modifier = Modifier.height(12.dp))
                Text(title, color = MarinesWhite, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(description, color = MarinesWhite.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Left, lineHeight = 20.sp)
            }
        } else {
            Column(modifier = Modifier.weight(1f).padding(start = 24.dp)) {
                Box(modifier = Modifier.width(30.dp).height(4.dp).background(MarinesRed))
                Spacer(modifier = Modifier.height(12.dp))
                Text(title, color = MarinesWhite, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(description, color = MarinesWhite.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Left, lineHeight = 20.sp)
            }
            Spacer(modifier = Modifier.width(20.dp))
            Image(
                painter = painterResource(id = resId),
                contentDescription = null,
                modifier = Modifier
                    .weight(1.1f)
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )
        }
    }
}

// --- PANTALLA: GACETA (ESTILO GALERÍA) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigitalNewspaperScreen(newsItems: List<NewsItem>, isLoading: Boolean, onBack: () -> Unit, onNewsClick: (NewsItem) -> Unit) {
    Scaffold(
        containerColor = MarinesBlack,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "GACETA IMB", 
                        fontWeight = FontWeight.Black, 
                        color = MarinesWhite, 
                        letterSpacing = 3.sp,
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = { 
                    IconButton(onClick = onBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MarinesWhite) 
                    } 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MarinesBlack)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                CircularProgressIndicator(color = MarinesRed) 
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(newsItems) { news ->
                    GalleryNewsItem(news, onClick = { onNewsClick(news) })
                }
            }
        }
    }
}

@Composable
fun GalleryNewsItem(news: NewsItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable { onClick() },
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
    ) {
        Box {
            AsyncImage(
                model = news.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Degradado de alto contraste para legibilidad superior
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f)),
                            startY = 150f
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(3.dp)
                        .background(MarinesRed)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = news.title.uppercase(),
                    color = MarinesWhite,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        lineHeight = 18.sp,
                        letterSpacing = 0.5.sp
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = news.category.uppercase(),
                    color = MarinesRed,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// --- PANTALLA: DETALLE NOTICIA ---
@Composable
fun AppleNewsDetail(news: NewsItem?, onBack: () -> Unit) {
    if (news == null) return
    Scaffold(containerColor = MarinesBlack) { padding ->
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                AsyncImage(model = news.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(0.3f), Color.Transparent, MarinesBlack))))
                IconButton(onClick = onBack, modifier = Modifier.padding(top = 50.dp, start = 16.dp).background(Color.Black.copy(0.5f), CircleShape)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MarinesWhite)
                }
            }
            Column(modifier = Modifier.padding(24.dp)) {
                Text(news.category.uppercase(), color = MarinesRed, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(news.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MarinesWhite, lineHeight = 36.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(news.date, color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), thickness = 0.5.dp, color = MarinesDarkGray)
                Text(news.content, style = MaterialTheme.typography.bodyLarge, lineHeight = 32.sp, color = Color.LightGray)
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

// --- PANTALLA: GALERÍA INSTITUCIONAL ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(onBack: () -> Unit) {
    var selectedImageRes by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current

    val galleryResources = listOf(
        R.drawable.imb_02,
        R.drawable.imb_03,
        R.drawable.imb_04,
        R.drawable.imb_05
    )

    Scaffold(
        containerColor = MarinesBlack,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "GALERÍA TÁCTICA", 
                        fontWeight = FontWeight.Black, 
                        color = MarinesWhite,
                        letterSpacing = 2.sp
                    ) 
                },
                navigationIcon = { 
                    IconButton(onClick = onBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MarinesWhite) 
                    } 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MarinesBlack)
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            contentPadding = PaddingValues(0.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            items(galleryResources) { resId ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clickable { selectedImageRes = resId }
                ) {
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)))
                    
                    // Detalle visual en la esquina
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(40.dp)
                            .background(MarinesRed.copy(alpha = 0.8f))
                    ) {
                        Icon(
                            Icons.Default.Fullscreen, 
                            null, 
                            tint = MarinesWhite, 
                            modifier = Modifier.align(Alignment.Center).size(24.dp)
                        )
                    }
                }
            }
        }
    }

    // Pantalla de imagen expandida (Dialog)
    if (selectedImageRes != null) {
        Dialog(
            onDismissRequest = { selectedImageRes = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // Imagen expandida
                Image(
                    painter = painterResource(id = selectedImageRes!!),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    contentScale = ContentScale.Fit
                )

                // Botones de acción
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .align(Alignment.TopEnd),
                    horizontalArrangement = Arrangement.End
                ) {
                    // Botón Descargar
                    IconButton(
                        onClick = {
                            saveImageToGallery(context, selectedImageRes!!)
                            Toast.makeText(context, "Imagen guardada en Galería", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.background(Color.Black.copy(0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Download, "Descargar", tint = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))

                    // Botón Cerrar
                    IconButton(
                        onClick = { selectedImageRes = null },
                        modifier = Modifier.background(Color.Black.copy(0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, "Cerrar", tint = Color.White)
                    }
                }
            }
        }
    }
}

// Función auxiliar para guardar imagen
fun saveImageToGallery(context: Context, resId: Int) {
    val bitmap = BitmapFactory.decodeResource(context.resources, resId)
    val filename = "IMB_${System.currentTimeMillis()}.jpg"
    var fos: OutputStream? = null
    
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            val imageUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            fos = imageUri?.let { context.contentResolver.openOutputStream(it) }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }

        fos?.use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

// Función para corregir URLs de GitHub
fun fixGitHubUrl(url: String): String {
    val trimmed = url.trim()
    return if (trimmed.contains("github.com") && trimmed.contains("/blob/")) {
        trimmed.replace("github.com", "raw.githubusercontent.com")
               .replace("/blob/", "/")
    } else {
        trimmed
    }
}

// --- PANTALLA: LECTOR DE REVISTA (ESTILO EBOOK) ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MagazineReaderScreen(magazine: MagazineItem, onBack: () -> Unit) {
    val pages = remember(magazine.fileUrl) {
        if (magazine.fileUrl.isNotEmpty()) {
            magazine.fileUrl.split(",").map { fixGitHubUrl(it) }
        } else {
            listOf(fixGitHubUrl(magazine.coverUrl))
        }
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color.Black.copy(0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
                Text(
                    text = magazine.title.uppercase(),
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 16.dp
            ) { page ->
                val pageUrl = pages[page].trim()
                if (pageUrl.isNotEmpty()) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(pageUrl)
                            .crossfade(true)
                            .listener(
                                onError = { _, result ->
                                    android.util.Log.e("MagazineReader", "Error loading page $page: ${result.throwable.message}")
                                }
                            )
                            .build(),
                        contentDescription = "Página ${page + 1}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        loading = {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MarinesRed)
                            }
                        },
                        error = { state ->
                            val errorMsg = state.result.throwable.message ?: "Error desconocido"
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Error, null, tint = MarinesRed, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Error al cargar página ${page + 1}", color = Color.White)
                                Text(errorMsg, color = Color.Gray, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
                            }
                        }
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("URL Vacía", color = Color.White)
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp),
                color = MarinesRed,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "PÁGINA ${pagerState.currentPage + 1} DE ${pages.size}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    color = MarinesWhite,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

// --- PANTALLA: REVISTA ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagazineShelfScreen(magazines: List<MagazineItem>, isLoading: Boolean, onBack: () -> Unit) {
    var selectedMagazine by remember { mutableStateOf<MagazineItem?>(null) }
    
    if (selectedMagazine != null) {
        MagazineReaderScreen(magazine = selectedMagazine!!, onBack = { selectedMagazine = null })
    } else {
        Scaffold(
            containerColor = MarinesBlack,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            "REVISTA IMB", 
                            fontWeight = FontWeight.Black, 
                            color = MarinesWhite, 
                            letterSpacing = 2.sp
                        ) 
                    },
                    navigationIcon = { 
                        IconButton(onClick = onBack) { 
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MarinesWhite) 
                        } 
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MarinesBlack)
                )
            }
        ) { padding ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                    CircularProgressIndicator(color = MarinesRed) 
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(padding)
                ) {
                    items(magazines) { magazine ->
                        Column(
                            modifier = Modifier.clickable { selectedMagazine = magazine }
                        ) {
                            Card(
                                shape = RoundedCornerShape(0.dp),
                                modifier = Modifier.fillMaxWidth().aspectRatio(0.7f),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Box {
                                    val coverUrl = fixGitHubUrl(magazine.coverUrl)
                                    if (coverUrl.isNotEmpty()) {
                                        SubcomposeAsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(coverUrl)
                                                .crossfade(true)
                                                .listener(
                                                    onError = { _, result ->
                                                        android.util.Log.e("MagazineShelf", "Error loading cover: ${result.throwable.message}")
                                                    }
                                                )
                                                .build(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                            loading = {
                                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(color = MarinesRed, modifier = Modifier.size(24.dp))
                                                }
                                            },
                                            error = { state ->
                                                val errorMsg = state.result.throwable.message ?: ""
                                                Box(Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Icon(Icons.Default.BrokenImage, null, tint = Color.Gray)
                                                        if (errorMsg.isNotEmpty()) {
                                                            Text(errorMsg, color = Color.Gray, fontSize = 8.sp, maxLines = 1)
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    } else {
                                        Box(Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) {
                                            Text("Sin Portada", color = Color.White, fontSize = 10.sp)
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(MarinesRed)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("EDICIÓN", color = MarinesWhite, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(modifier = Modifier.width(20.dp).height(2.dp).background(MarinesRed))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = magazine.title.uppercase(),
                                color = MarinesWhite,
                                fontWeight = FontWeight.Black,
                                maxLines = 2,
                                style = MaterialTheme.typography.labelLarge,
                                lineHeight = 18.sp
                            )
                            Text(
                                text = magazine.date,
                                color = Color.Gray,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- PANTALLA: CÓDIGO DE HONOR ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HonorCodeScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MarinesBlack,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("CÓDIGO DE HONOR", fontWeight = FontWeight.Black, color = MarinesWhite) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MarinesWhite) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MarinesBlack)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
            Text("NUESTROS PRINCIPIOS", color = MarinesRed, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("El conjunto de valores éticos que rigen al Infante de Marina Bolivariana.", style = MaterialTheme.typography.bodyLarge, color = MarinesWhite, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            val principles = getPrinciples()
            principles.forEach { principle ->
                Card(modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(), shape = RoundedCornerShape(2.dp), colors = CardDefaults.cardColors(containerColor = MarinesDarkGray)) {
                    Text(text = principle, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = Color.LightGray, lineHeight = 24.sp)
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// --- COMPONENTES REUTILIZABLES ---
@Composable
fun ExploreCard(title: String, subtitle: String, imageUrl: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .fillMaxWidth()
            .height(240.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(0.dp)
    ) {
        Box {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                            startY = 100f
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Box(modifier = Modifier.width(40.dp).height(4.dp).background(MarinesRed))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = title.uppercase(),
                    color = MarinesWhite,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.headlineSmall,
                    letterSpacing = 2.sp
                )
                Text(
                    text = subtitle,
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SmallExploreCard(modifier: Modifier, title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(modifier = modifier.height(110.dp).clickable { onClick() }, shape = RoundedCornerShape(4.dp), colors = CardDefaults.cardColors(containerColor = MarinesDarkGray)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = MarinesRed, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = MarinesWhite, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun AppleFeaturedCard(news: NewsItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(300.dp)
            .height(400.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(0.dp)
    ) {
        Box {
            AsyncImage(
                model = news.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f)),
                            startY = 200f
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Box(modifier = Modifier.width(20.dp).height(3.dp).background(MarinesRed))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = news.category.uppercase(),
                    color = MarinesRed,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = news.title.uppercase(),
                    color = MarinesWhite,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 3,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, onSeeAll: (() -> Unit)?, isDark: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = if (isDark) MarinesWhite else Color.Black, letterSpacing = 1.sp)
        if (onSeeAll != null) Text(text = "VER TODO", color = MarinesRed, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, modifier = Modifier.clickable { onSeeAll() })
    }
}

// --- DATA HELPERS ---
fun getPrinciples() = listOf(
    "1) Soy un Infante de Marina venezolano, heredero de las armas y glorias del Ejército Libertador, orgulloso de mi unidad y como tal fiel a mi juramento de defender la patria y sus instituciones hasta perder la vida y no abandonar jamás a mis superiores.",
    "2) Soy discreto, serio e incorruptible, seré cuidadoso en mi conversación para no dar información que pueda comprometer la seguridad o prestigio de la institución.",
    "3) La lealtad a la Infantería de Marina y a todo cuanto a ella representa, estará por encima de cualquier otra consideración.",
    "4) Seré siempre cortés y caballeroso con mis superiores, iguales, subalternos, especialmente con ancianos y mujeres.",
    "5) La serenidad, la prudencia, la reflexión, la resolución, el valor militar y la voluntad de combatir, siempre a la ofensiva, me darán la superioridad en el campo de batalla.",
    "6) Ante el enemigo lo más preciado son las municiones. El que sólo dispara para tranquilizarse, carece de valor y no merece el título de Infante de Marina.",
    "7) No me rendiré nunca, el recuerdo de mis Libertadores avivará en mí, voluntad de resistir.",
    "8) Sólo podré vencer si dispongo de buenas armas. Las cuidaré aplicando el principio primero mis armas y después yo.",
    "9) Debo conocer lo esencial de cada operación para poder seguir actuando inteligentemente si mi jefe muere.",
    "10) Lucharé caballerosamente contra un adversario honorable, a los francotiradores y traidores, los trataré con energía y resolución.",
    "11) Siempre con los ojos bien abiertos, ágil como un lebrel, resistente como el acero.",
    "12) Confío en mi Comandante como él en mí, he sido adiestrado para combatir y subsistir. Primero muerto el enemigo antes que yo.",
    "13) Venezuela: Patria querida, en ti creo y por ti moriré para mantener tu libertad, independencia, soberanía, ley y orden."
)

fun getFullUnitSections() = listOf(
    UnitSection("Comando y Unidades de Apoyo General", listOf("CIMB - Comando de Infantería de Marina Bolivariana", "BATAPIMB - Batallón de Apoyo")),
    UnitSection("1ra. Brigada Anfibia", listOf("BRIMB1 - CN. Manuel Ponte Rodríguez", "BIMB11 - Gral. Rafael Urdaneta", "BAVAIMB12 - CC. Miguel Ponce Lugo", "GRART13 - Gral. Agustín Codazzi", "BATAP14 - CA. José Ramón Yépez")),
    UnitSection("2da. Brigada Anfibia", listOf("BRIMB2 - CA. José Eugenio Hernández", "BIMB21 - Gral. José Francisco Bermúdez", "BIMB22 - Mcal. Antonio José de Sucre")),
    UnitSection("5ta. Brigada Fluvial", listOf("BRIMBF5 - CF. José Tomás Machado", "EFIMBANDI - CN. Antonio Díaz", "COFIMB51 - G/B Daniel Florencio O’Leary")),
    UnitSection("8va. Brigada Operaciones Especiales", listOf("BRIOPEM8 - Generalísimo Francisco de Miranda", "UOPE81 - CC. Henry Lilong García", "UOPE82 - GJ. Jose Félix Rivas", "UOPE83 - Cacique Guaicaipuro"))
)
