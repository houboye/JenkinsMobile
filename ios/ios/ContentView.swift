//
//  ContentView.swift
//  ios
//
//  Main navigation root
//

import SwiftUI

struct ContentView: View {
    @StateObject private var storage = StorageService.shared
    @State private var isLoggedIn = false
    
    var body: some View {
        Group {
            if isLoggedIn {
                DashboardView()
            } else {
                LoginView(isLoggedIn: $isLoggedIn)
            }
        }
        .onAppear {
            storage.restoreSession()
            isLoggedIn = storage.isLoggedIn
        }
        .onChange(of: storage.isLoggedIn) { _, newValue in
            isLoggedIn = newValue
        }
    }
}

#Preview {
    ContentView()
}
