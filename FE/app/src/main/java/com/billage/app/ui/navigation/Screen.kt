package com.billage.app.ui.navigation
// 앱 안의 모든 화면 경로(주소)를 한 곳에서 관리하는 파일
// 예) 웹: "/home", "/login" → 앱: Screen.Home.route, Screen.Login.route

sealed class Screen(val route: String) {
    // 홈 화면
    object Home : Screen("home")
    // 앞으로 화면이 추가될 때 아래에 계속 추가
    // TODO: 로그인, 계약상세 등 화면 추가 예정
}