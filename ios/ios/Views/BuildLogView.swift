//
//  BuildLogView.swift
//  ios
//
//  View for displaying build console output
//

import SwiftUI

struct BuildLogView: View {
    let jobName: String
    let buildNumber: Int
    let buildURL: String
    
    @StateObject private var viewModel: BuildLogViewModel
    @Environment(\.dismiss) private var dismiss
    
    init(jobName: String, buildNumber: Int, buildURL: String) {
        self.jobName = jobName
        self.buildNumber = buildNumber
        self.buildURL = buildURL
        _viewModel = StateObject(wrappedValue: BuildLogViewModel(buildURL: buildURL))
    }
    
    var body: some View {
        NavigationStack {
            ZStack {
                Color(red: 0.12, green: 0.12, blue: 0.12)
                    .ignoresSafeArea()
                
                if viewModel.isLoading {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                } else if let error = viewModel.errorMessage {
                    VStack(spacing: 12) {
                        Image(systemName: "exclamationmark.triangle")
                            .font(.system(size: 40))
                            .foregroundColor(.orange)
                        Text("加载失败")
                            .font(.headline)
                            .foregroundColor(.white)
                        Text(error)
                            .font(.subheadline)
                            .foregroundColor(.gray)
                            .multilineTextAlignment(.center)
                        
                        Button("重试") {
                            Task {
                                await viewModel.loadLog()
                            }
                        }
                        .buttonStyle(.bordered)
                        .tint(.blue)
                        .padding(.top, 8)
                    }
                    .padding()
                } else if let logContent = viewModel.logContent {
                    ScrollViewReader { proxy in
                        ScrollView([.horizontal, .vertical]) {
                            Text(logContent)
                                .font(.system(size: 12, design: .monospaced))
                                .foregroundColor(Color(red: 0.83, green: 0.83, blue: 0.83))
                                .padding(12)
                                .textSelection(.enabled)
                                .id("logBottom")
                        }
                        .onAppear {
                            // Scroll to bottom on load
                            proxy.scrollTo("logBottom", anchor: .bottom)
                        }
                    }
                } else {
                    Text("暂无日志")
                        .foregroundColor(.gray)
                }
            }
            .navigationTitle("构建日志")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("关闭") {
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    HStack(spacing: 12) {
                        // Copy button
                        Button {
                            if let log = viewModel.logContent {
                                UIPasteboard.general.string = log
                            }
                        } label: {
                            Image(systemName: "doc.on.doc")
                        }
                        .disabled(viewModel.logContent == nil)
                        
                        // Refresh button
                        Button {
                            Task {
                                await viewModel.loadLog()
                            }
                        } label: {
                            Image(systemName: "arrow.clockwise")
                        }
                        .disabled(viewModel.isLoading)
                    }
                }
                
                ToolbarItem(placement: .principal) {
                    VStack(spacing: 2) {
                        Text("构建日志")
                            .font(.headline)
                        Text("\(jobName) #\(buildNumber)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            }
        }
        .task {
            await viewModel.loadLog()
        }
    }
}

@MainActor
class BuildLogViewModel: ObservableObject {
    @Published var logContent: String?
    @Published var isLoading = false
    @Published var errorMessage: String?
    
    private let buildURL: String
    private let api = JenkinsAPI.shared
    
    init(buildURL: String) {
        self.buildURL = buildURL
    }
    
    func loadLog() async {
        isLoading = true
        errorMessage = nil
        
        do {
            let log = try await api.fetchConsoleOutputByURL(buildURL: buildURL)
            logContent = log
        } catch let error as APIError {
            errorMessage = error.localizedDescription
        } catch {
            errorMessage = error.localizedDescription
        }
        
        isLoading = false
    }
}

#Preview {
    BuildLogView(
        jobName: "test-job",
        buildNumber: 123,
        buildURL: "http://example.com/job/test/123/"
    )
}

