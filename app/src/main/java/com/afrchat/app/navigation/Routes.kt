package com.afrchat.app.navigation

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val FORGOT_PASSWORD = "forgot_password"

    const val MAIN = "main"
    const val SEARCH = "search"
    const val CREATE_GROUP = "create_group"
    const val CREATE_STORY = "create_story"

    const val CHAT = "chat/{conversationId}/{peerName}"
    fun chat(conversationId: String, peerName: String) =
        "chat/$conversationId/$peerName"

    const val GROUP_INFO = "group_info/{groupId}/{conversationId}"
    fun groupInfo(groupId: String, conversationId: String) =
        "group_info/$groupId/$conversationId"

    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit_profile"
    const val SETTINGS = "settings"

    const val STORY_VIEWER = "story_viewer/{ownerUid}"
    fun storyViewer(ownerUid: String) =
        "story_viewer/$ownerUid"

    const val ADMIN_DASHBOARD = "admin_dashboard"
    const val ADMIN_USERS = "admin_users"
    const val ADMIN_REPORTS = "admin_reports"
}
