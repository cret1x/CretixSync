package com.cretix.cretixsync

import android.net.Uri
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class AlbumItem(var id: Long, val name: String, var selected: Boolean, val thumbnail: Uri) :
    Parcelable