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

package com.twoeightnine.root.xvii.wall.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.twoeightnine.root.xvii.model.*
import com.twoeightnine.root.xvii.network.ApiService
import com.twoeightnine.root.xvii.network.response.WallPostResponse
import com.twoeightnine.root.xvii.search.SearchDialog
import com.twoeightnine.root.xvii.utils.subscribeSmart
import javax.inject.Inject

class WallViewModel(private val api: ApiService) : ViewModel() {

    private val wallLiveData = WrappedMutableLiveData<ArrayList<WallPost>>()
    fun getWall() = wallLiveData as WrappedLiveData<ArrayList<WallPost>>

    fun loadWall(ownerId:Int, offset: Int = 0) {
        api.getWall(ownerId, COUNT, offset)
                .subscribeSmart({ response ->
                    val existing = if (offset == 0) {
                        arrayListOf()
                    } else {
                        wallLiveData.value?.data ?: arrayListOf()
                    }
                    response.items?.forEach { wallPost ->
                        getData(wallPost, response)
                    }

                    existing.addAll(response.items)
                    wallLiveData.value = Wrapper(existing)
                }, { error ->
                    wallLiveData.value = Wrapper(error = error)
                })
    }

    private fun getData(wallPost:WallPost, response:WallPostResponse){
        wallPost.group = getGroup(-wallPost.fromId, response)
        wallPost.user = getUser(wallPost.fromId, response)
        if (wallPost.copyHistory != null && wallPost.copyHistory.size > 0) {
            getData(wallPost.copyHistory[0], response)
        }
    }
    private fun getGroup(fromId: Int, response: WallPostResponse): Group {
        for (group in response.groups) {
            if (group.id == fromId) {
                return group
            }
        }
        return Group()
    }

    private fun getUser(fromId: Int, response: WallPostResponse): User {
        for (user in response.profiles) {
            if (user.id == fromId) {
                return user
            }
        }
        return User()
    }

    companion object {
        const val COUNT = 15
    }

    class Factory @Inject constructor(private val api: ApiService) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) = WallViewModel(api) as T
    }
}