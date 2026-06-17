package com.robinzon.medicationwizard.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.robinzon.medicationwizard.R

/**
 * A temporary preview tool to visualize the re-created Owl Doctor mascot.
 */
@Preview(showBackground = true)
@Composable
fun MascotMockupPreview() {
    Box(
        modifier = Modifier
            .size(512.dp)
            .background(Color(0xFF1A1C1E)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_mascot_mockup),
            contentDescription = "Owl Doctor Re-creation Mockup",
            modifier = Modifier.size(400.dp)
        )
    }
}
