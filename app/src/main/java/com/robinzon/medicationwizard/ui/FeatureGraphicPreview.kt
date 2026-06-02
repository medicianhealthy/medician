package com.robinzon.medicationwizard.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.robinzon.medicationwizard.R

/**
 * A temporary preview tool to visualize the Feature Graphic.
 */
@Preview(showBackground = true, widthDp = 1024, heightDp = 500)
@Composable
fun FeatureGraphicPreview() {
    Box(
        modifier = Modifier.size(width = 1024.dp, height = 500.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.feature_graphic),
            contentDescription = "Medication Wizard Feature Graphic",
            modifier = Modifier.size(width = 1024.dp, height = 500.dp)
        )
    }
}
