package com.algo1127.mytask.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryUtils {
    val icons = mapOf(
        "Brush" to Icons.Default.Brush,
        "School" to Icons.Default.School,
        "Favorite" to Icons.Default.Favorite,
        "Business" to Icons.Default.Business,
        "Task" to Icons.Default.Task,
        "Schedule" to Icons.Default.Schedule,
        "Event" to Icons.Default.Event,
        "CheckCircle" to Icons.Default.CheckCircle,
        "Home" to Icons.Default.Home,
        "Work" to Icons.Default.Work,
        "FitnessCenter" to Icons.Default.FitnessCenter,
        "ShoppingBag" to Icons.Default.ShoppingBag,
        "Restaurant" to Icons.Default.Restaurant,
        "LocalCafe" to Icons.Default.LocalCafe,
        "Movie" to Icons.Default.Movie,
        "Flight" to Icons.Default.Flight,
        "DirectionsCar" to Icons.Default.DirectionsCar,
        "LocalLibrary" to Icons.Default.LocalLibrary,
        "Code" to Icons.Default.Code,
        "SelfCare" to Icons.Default.SelfImprovement,
        "Medical" to Icons.Default.MedicalServices,
        "Bank" to Icons.Default.AccountBalance,
        "Group" to Icons.Default.Group,
        "Pets" to Icons.Default.Pets,
        "Star" to Icons.Default.Star,
        "Lock" to Icons.Default.Lock,
        "Build" to Icons.Default.Build,
        "Language" to Icons.Default.Language,
        "BugReport" to Icons.Default.BugReport,
        "Extension" to Icons.Default.Extension,
        "AccountCircle" to Icons.Default.AccountCircle,
        "Anchor" to Icons.Default.Anchor,
        "Announcement" to Icons.Default.Announcement,
        "Bookmark" to Icons.Default.Bookmark,
        "CameraAlt" to Icons.Default.CameraAlt,
        "Chat" to Icons.Default.Chat,
        "Cloud" to Icons.Default.Cloud,
        "Computer" to Icons.Default.Computer,
        "CreditCard" to Icons.Default.CreditCard,
        "Eco" to Icons.Default.Eco,
        "Explore" to Icons.Default.Explore,
        "Face" to Icons.Default.Face,
        "Flag" to Icons.Default.Flag,
        "FlashOn" to Icons.Default.FlashOn,
        "Games" to Icons.Default.Games,
        "Gavel" to Icons.Default.Gavel,
        "Gift" to Icons.Default.CardGiftcard,
        "Grade" to Icons.Default.Grade,
        "HealthAndSafety" to Icons.Default.HealthAndSafety,
        "Hotel" to Icons.Default.Hotel,
        "Info" to Icons.Default.Info,
        "Key" to Icons.Default.Key,
        "Lightbulb" to Icons.Default.Lightbulb,
        "LocalFlorist" to Icons.Default.LocalFlorist,
        "LocalGasStation" to Icons.Default.LocalGasStation,
        "LocalMall" to Icons.Default.LocalMall,
        "LocalPostOffice" to Icons.Default.LocalPostOffice,
        "LocalShipping" to Icons.Default.LocalShipping,
        "Map" to Icons.Default.Map,
        "Mic" to Icons.Default.Mic,
        "MusicNote" to Icons.Default.MusicNote,
        "Notifications" to Icons.Default.Notifications,
        "Palette" to Icons.Default.Palette,
        "Park" to Icons.Default.Park,
        "Phone" to Icons.Default.Phone,
        "Public" to Icons.Default.Public,
        "Receipt" to Icons.Default.Receipt,
        "School" to Icons.Default.School,
        "Search" to Icons.Default.Search,
        "Security" to Icons.Default.Security,
        "Send" to Icons.Default.Send,
        "Settings" to Icons.Default.Settings,
        "Share" to Icons.Default.Share,
        "SmartToy" to Icons.Default.SmartToy,
        "SportsBasketball" to Icons.Default.SportsBasketball,
        "SportsEsports" to Icons.Default.SportsEsports,
        "Store" to Icons.Default.Store,
        "LightMode" to Icons.Default.LightMode,
        "Support" to Icons.Default.Support,
        "Sync" to Icons.Default.Sync,
        "ThumbUp" to Icons.Default.ThumbUp,
        "Timer" to Icons.Default.Timer,
        "Traffic" to Icons.Default.Traffic,
        "Train" to Icons.Default.Train,
        "Tv" to Icons.Default.Tv,
        "Videocam" to Icons.Default.Videocam,
        "Visibility" to Icons.Default.Visibility,
        "Warning" to Icons.Default.Warning,
        "Watch" to Icons.Default.Watch,
        "WaterDrop" to Icons.Default.WaterDrop,
        "Weekend" to Icons.Default.Weekend,
        "Wifi" to Icons.Default.Wifi,
        "Wolt" to Icons.Default.TakeoutDining,
        "WorkHistory" to Icons.Default.WorkHistory
    )

    fun getIcon(name: String): ImageVector = icons[name] ?: Icons.Default.Circle

    fun getIconName(icon: ImageVector): String {
        return icons.entries.find { it.value == icon }?.key ?: "Circle"
    }

    val colors = listOf(
        Theme.Teal, Theme.Blue, Theme.Purple, Theme.Gold, Theme.Rose, Theme.Emerald, Theme.Orange,
        Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7), Color(0xFF3F51B5), Color(0xFF2196F3),
        Color(0xFF03A9F4), Color(0xFF00BCD4), Color(0xFF009688), Color(0xFF4CAF50), Color(0xFF8BC34A),
        Color(0xFFCDDC39), Color(0xFFFFEB3B), Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFFF5722)
    )

    fun colorToHex(color: Color): String {
        val argb = color.toArgb()
        return String.format("#%08X", argb)
    }

    fun hexToColor(hex: String): Color {
        return try {
            val sanitizedHex = if (hex.startsWith("#")) hex else "#$hex"
            Color(android.graphics.Color.parseColor(sanitizedHex))
        } catch (e: Exception) {
            android.util.Log.e("CategoryUtils", "Failed to parse hex color: $hex", e)
            Theme.Teal
        }
    }
}
