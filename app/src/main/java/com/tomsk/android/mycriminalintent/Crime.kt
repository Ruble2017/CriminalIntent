package com.tomsk.android.mycriminalintent

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

//data class Crime(val id: UUID = UUID.randomUUID(),
//                 var title: String = "",
//                 var date: Date = Date(),
//                 var isSolved: Boolean = false)

@Entity
data class Crime(@PrimaryKey val id: UUID = UUID.randomUUID(),
                 var title: String = "",
                 var date: Date = Date(),
                 var isSolved: Boolean = false,
                 var suspect: String = ""){

    val photoFileName
                get() = "IMG_$id.jpg"
}