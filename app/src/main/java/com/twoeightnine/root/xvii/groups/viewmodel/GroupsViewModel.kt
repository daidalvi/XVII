/*
 * xvii - messenger for vk
 * Copyright (C) 2021  TwoEightNine
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.twoeightnine.root.xvii.groups.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.twoeightnine.root.xvii.App
import com.twoeightnine.root.xvii.model.*
import com.twoeightnine.root.xvii.network.ApiService
import com.twoeightnine.root.xvii.utils.subscribeSmart
import javax.inject.Inject

class GroupsViewModel(private val api: ApiService) : ViewModel() {

    private val data: SharedPreferences by lazy {
        App.context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    }

    private val groupsLiveData = WrappedMutableLiveData<ArrayList<Group>>()

    fun getGroups() = groupsLiveData as WrappedLiveData<ArrayList<Group>>

    fun loadGroups(offset: Int = 0) {
        api.getGroupList(COUNT, offset)
                .subscribeSmart({ groups ->
                    val existing = if (offset == 0) {
                        arrayListOf()
                    } else {
                        groupsLiveData.value?.data ?: arrayListOf()
                    }
                    existing.addAll(groups.items)
                    groupsLiveData.value = Wrapper(existing)
                }, { error ->
                    groupsLiveData.value = Wrapper(error = error)
                })
    }

    var starredIds:   Set<String>
        get() = data.getStringSet(STARRED, setOf<String>()) as Set<String>
        set(value) = data.edit().putStringSet(STARRED, value).apply()

    fun toggleStarred(id: String, isStarred: Boolean) {
        val starred = starredIds.toMutableSet()
        if(isStarred){
            starred.remove(id)
        }else{
            starred.add(id)
        }
        starredIds = starred.toSet()
    }

    companion object {
        const val COUNT = 50
        const val NAME = "groupsPrefs"
        const val STARRED = "starred"
    }

    class Factory @Inject constructor(private val api: ApiService) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) = GroupsViewModel(api) as T
    }
}