package com.tncnursing.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tncnursing.app.ui.components.CourseCard
import com.tncnursing.app.ui.theme.TncNavyPrimary
import com.tncnursing.app.ui.viewmodel.MainViewModel
import com.tncnursing.app.ui.viewmodel.UiState

@Composable
fun CoursesScreen(
    viewModel: MainViewModel,
    onCourseClick: (String) -> Unit
) {
    val coursesState by viewModel.coursesState.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val bookmarkedIds = bookmarks.map { it.targetId }.toSet()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "NORCET", "ESIC / RRB", "Core Subjects", "Specialty")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Target Study Batches",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search batches by subject or exam...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val selected = selectedCategory == category
                FilterChip(
                    selected = selected,
                    onClick = { selectedCategory = category },
                    label = { Text(category, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TncNavyPrimary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val state = coursesState) {
            is UiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = TncNavyPrimary)
                }
            }
            is UiState.Success -> {
                val filtered = state.data.filter { course ->
                    val matchesQuery = searchQuery.isBlank() ||
                        course.name.contains(searchQuery, ignoreCase = true) ||
                        course.description.contains(searchQuery, ignoreCase = true)
                    val matchesCategory = selectedCategory == "All" || course.category == selectedCategory
                    matchesQuery && matchesCategory
                }

                if (filtered.isEmpty()) {
                    Text("No study batches found matching filter.", modifier = Modifier.padding(top = 24.dp))
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(filtered) { course ->
                            CourseCard(
                                course = course,
                                isBookmarked = bookmarkedIds.contains(course.rowId),
                                onCourseClick = { onCourseClick(course.rowId) },
                                onBookmarkToggle = {
                                    viewModel.toggleBookmark(
                                        targetId = course.rowId,
                                        type = "course",
                                        title = course.name,
                                        subtitle = course.category,
                                        imageUrl = course.imageUrl
                                    )
                                }
                            )
                        }
                    }
                }
            }
            is UiState.Error -> {
                Text("Failed to load courses.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
