//
//  SettingsView.swift
//  ios
//
//  Settings screen
//

import SwiftUI

struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var storage = StorageService.shared
    
    @State private var showLogoutAlert = false
    
    var body: some View {
        NavigationStack {
            List {
                // Server Info Section
                Section {
                    HStack {
                        Label("服务器", systemImage: "globe")
                        Spacer()
                        Text(storage.server.url)
                            .foregroundColor(.secondary)
                            .lineLimit(1)
                    }
                    
                    HStack {
                        Label("用户名", systemImage: "person")
                        Spacer()
                        Text(storage.server.username)
                            .foregroundColor(.secondary)
                    }
                } header: {
                    Text("服务器配置")
                }
                
                // App Info Section
                Section {
                    HStack {
                        Label("版本", systemImage: "info.circle")
                        Spacer()
                        Text("1.0.0")
                            .foregroundColor(.secondary)
                    }
                    
                    HStack {
                        Label("构建号", systemImage: "hammer")
                        Spacer()
                        Text("1")
                            .foregroundColor(.secondary)
                    }
                } header: {
                    Text("应用信息")
                }
                
                // Actions Section
                Section {
                    Button(role: .destructive) {
                        showLogoutAlert = true
                    } label: {
                        HStack {
                            Label("退出登录", systemImage: "rectangle.portrait.and.arrow.right")
                            Spacer()
                        }
                    }
                }
            }
            .navigationTitle("设置")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("完成") {
                        dismiss()
                    }
                }
            }
            .alert("退出登录", isPresented: $showLogoutAlert) {
                Button("取消", role: .cancel) {}
                Button("退出", role: .destructive) {
                    storage.logout()
                    dismiss()
                }
            } message: {
                Text("确定要退出登录吗？")
            }
        }
    }
}

#Preview {
    SettingsView()
}

