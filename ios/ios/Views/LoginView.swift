//
//  LoginView.swift
//  ios
//
//  Login screen
//

import SwiftUI

struct LoginView: View {
    @StateObject private var viewModel = LoginViewModel()
    @Binding var isLoggedIn: Bool
    
    @State private var isPasswordVisible = false
    
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 32) {
                    // Logo and Title
                    VStack(spacing: 16) {
                        Image(systemName: "server.rack")
                            .font(.system(size: 72))
                            .foregroundStyle(
                                LinearGradient(
                                    colors: [.blue, .cyan],
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                        
                        Text("Jenkins Mobile")
                            .font(.system(size: 32, weight: .bold, design: .rounded))
                            .foregroundColor(.primary)
                        
                        Text("连接您的 Jenkins 服务器")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .padding(.top, 40)
                    
                    // Form Fields
                    VStack(spacing: 20) {
                        // Server URL
                        VStack(alignment: .leading, spacing: 8) {
                            Label("服务器地址", systemImage: "globe")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            
                            TextField("https://jenkins.example.com", text: $viewModel.serverURL)
                                .textFieldStyle(.plain)
                                .keyboardType(.URL)
                                .autocapitalization(.none)
                                .autocorrectionDisabled()
                                .padding()
                                .background(Color(.systemGray6))
                                .cornerRadius(12)
                        }
                        
                        // Username
                        VStack(alignment: .leading, spacing: 8) {
                            Label("用户名", systemImage: "person")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            
                            TextField("输入用户名", text: $viewModel.username)
                                .textFieldStyle(.plain)
                                .autocapitalization(.none)
                                .autocorrectionDisabled()
                                .padding()
                                .background(Color(.systemGray6))
                                .cornerRadius(12)
                        }
                        
                        // API Token
                        VStack(alignment: .leading, spacing: 8) {
                            Label("API Token", systemImage: "key")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            
                            HStack {
                                if isPasswordVisible {
                                    TextField("输入 API Token", text: $viewModel.apiToken)
                                        .textFieldStyle(.plain)
                                        .autocapitalization(.none)
                                        .autocorrectionDisabled()
                                } else {
                                    SecureField("输入 API Token", text: $viewModel.apiToken)
                                        .textFieldStyle(.plain)
                                }
                                
                                Button {
                                    isPasswordVisible.toggle()
                                } label: {
                                    Image(systemName: isPasswordVisible ? "eye.slash" : "eye")
                                        .foregroundColor(.secondary)
                                }
                            }
                            .padding()
                            .background(Color(.systemGray6))
                            .cornerRadius(12)
                        }
                    }
                    .padding(.horizontal)
                    
                    // Login Button
                    Button {
                        Task {
                            let success = await viewModel.login()
                            if success {
                                isLoggedIn = true
                            }
                        }
                    } label: {
                        HStack {
                            if viewModel.isLoading {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            } else {
                                Text("登录")
                                    .fontWeight(.semibold)
                            }
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(
                            viewModel.isFormValid ?
                            LinearGradient(
                                colors: [.blue, .cyan],
                                startPoint: .leading,
                                endPoint: .trailing
                            ) :
                            LinearGradient(
                                colors: [.gray, .gray],
                                startPoint: .leading,
                                endPoint: .trailing
                            )
                        )
                        .foregroundColor(.white)
                        .cornerRadius(12)
                    }
                    .disabled(!viewModel.isFormValid || viewModel.isLoading)
                    .padding(.horizontal)
                    
                    // Help Text
                    VStack(spacing: 8) {
                        Text("如何获取 API Token?")
                            .font(.footnote)
                            .foregroundColor(.secondary)
                        
                        Text("Jenkins → 用户 → 设置 → API Token")
                            .font(.caption)
                            .foregroundColor(.blue)
                    }
                    .padding(.top, 16)
                    
                    Spacer(minLength: 40)
                }
            }
            .background(Color(.systemBackground))
            .alert("错误", isPresented: $viewModel.showError) {
                Button("确定", role: .cancel) {}
            } message: {
                Text(viewModel.errorMessage ?? "未知错误")
            }
        }
    }
}

#Preview {
    LoginView(isLoggedIn: .constant(false))
}

