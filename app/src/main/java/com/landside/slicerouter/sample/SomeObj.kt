package com.landside.slicerouter.sample

import android.os.Parcel
import android.os.Parcelable

data class SomeObj(
    val name: String = ""
) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readString() ?: "") {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SomeObj> {
        override fun createFromParcel(parcel: Parcel): SomeObj {
            return SomeObj(parcel)
        }

        override fun newArray(size: Int): Array<SomeObj?> {
            return arrayOfNulls(size)
        }
    }
}