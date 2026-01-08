//
//  BuildDetailView.swift
//  ios
//
//  Build detail view with status, log, parameters, rebuild and delete
//

import SwiftUI

struct BuildDetailView: View {
    let jobName: String
    let jobURL: String
    let buildNumber: Int
    let buildURL: String
    
    @StateObject private var viewModel: BuildDetailViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var selectedTab = 0
    
    init(jobName: String, jobURL: String, buildNumber: Int, buildURL: String) {
        self.jobName = jobName
        self.jobURL = jobURL
        self.buildNumber = buildNumber
        self.buildURL = buildURL
        _viewModel = StateObject(wrappedValue: BuildDetailViewModel(
            jobName: jobName,
            jobURL: jobURL,
            buildNumber: buildNumber,
            buildURL: buildURL
        ))
    }
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Tab Picker
                Picker("", selection: $selectedTab) {
                    Text("详情").tag(0)
                    Text("日志").tag(1)
                    Text("参数").tag(2)
                }
                .pickerStyle(.segmented)
                .padding()
                
                // Content
                TabView(selection: $selectedTab) {
                    BuildInfoTab(viewModel: viewModel)
                        .tag(0)
                    
                    BuildLogTab(viewModel: viewModel)
                        .tag(1)
                    
                    BuildParametersTab(parameters: viewModel.buildDetail?.buildParameters ?? [])
                        .tag(2)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle("构建详情")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("关闭") {
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .principal) {
                    VStack(spacing: 2) {
                        Text("构建详情")
                            .font(.headline)
                        Text("\(jobName) #\(buildNumber)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        Task {
                            await viewModel.loadBuildDetail()
                        }
                    } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                    .disabled(viewModel.isLoading)
                }
            }
            .alert("错误", isPresented: $viewModel.showError) {
                Button("确定", role: .cancel) {}
            } message: {
                Text(viewModel.errorMessage ?? "未知错误")
            }
            .alert("提示", isPresented: $viewModel.showActionAlert) {
                Button("确定", role: .cancel) {
                    if viewModel.shouldDismiss {
                        dismiss()
                    }
                }
            } message: {
                Text(viewModel.actionMessage ?? "")
            }
            .confirmationDialog("确认删除", isPresented: $viewModel.showDeleteConfirm, titleVisibility: .visible) {
                Button("删除", role: .destructive) {
                    Task {
                        await viewModel.deleteBuild()
                    }
                }
                Button("取消", role: .cancel) {}
            } message: {
                Text("确定要删除构建 #\(buildNumber) 吗？此操作不可恢复。")
            }
            .sheet(isPresented: $viewModel.showRebuildSheet) {
                BuildParametersView(
                    jobName: jobName,
                    parameters: viewModel.rebuildParameters,
                    onBuild: { params in
                        viewModel.rebuild(with: params)
                    },
                    onCancel: {}
                )
            }
        }
        .task {
            await viewModel.loadBuildDetail()
        }
        .onChange(of: selectedTab) { _, newValue in
            if newValue == 1 && viewModel.logContent == nil && !viewModel.isLoadingLog {
                Task {
                    await viewModel.loadLog()
                }
            }
        }
    }
}

// MARK: - Build Info Tab

struct BuildInfoTab: View {
    @ObservedObject var viewModel: BuildDetailViewModel
    
    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                if viewModel.isLoading {
                    ProgressView()
                        .padding(.top, 40)
                } else if let buildDetail = viewModel.buildDetail {
                    let build = buildDetail.toBuild()
                    
                    // Status Card
                    VStack(spacing: 16) {
                        HStack(spacing: 16) {
                            BuildStatusIcon(result: build.buildResult, size: 48)
                            
                            VStack(alignment: .leading, spacing: 4) {
                                Text(build.buildResult.displayName)
                                    .font(.title2)
                                    .fontWeight(.bold)
                                    .foregroundColor(build.buildResult.color)
                                
                                Text("#\(build.number)")
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                            }
                            
                            Spacer()
                        }
                        
                        Divider()
                        
                        // Info rows
                        VStack(spacing: 12) {
                            InfoRow(label: "开始时间", value: build.detailedTimestamp)
                            InfoRow(label: "构建耗时", value: build.formattedDuration)
                            if let description = buildDetail.description, !description.isEmpty {
                                InfoRow(label: "描述", value: description)
                            }
                        }
                    }
                    .padding()
                    .background(Color(.systemBackground))
                    .cornerRadius(12)
                    
                    // Action Buttons
                    VStack(spacing: 12) {
                        // Rebuild Button
                        Button {
                            viewModel.showRebuildDialog()
                        } label: {
                            HStack {
                                if viewModel.isRebuilding {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                } else {
                                    Image(systemName: "arrow.counterclockwise")
                                }
                                Text(viewModel.isRebuilding ? "正在触发..." : "重新构建")
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color.blue)
                            .foregroundColor(.white)
                            .cornerRadius(10)
                        }
                        .disabled(viewModel.isRebuilding || viewModel.isDeleting)
                        
                        // Delete Button
                        Button {
                            viewModel.showDeleteConfirm = true
                        } label: {
                            HStack {
                                if viewModel.isDeleting {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .red))
                                } else {
                                    Image(systemName: "trash")
                                }
                                Text(viewModel.isDeleting ? "删除中..." : "删除构建")
                            }
                            .frame(maxWidth: .infinity)
                            .padding()
                            .background(Color(.systemBackground))
                            .foregroundColor(.red)
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(Color.red, lineWidth: 1)
                            )
                            .cornerRadius(10)
                        }
                        .disabled(viewModel.isDeleting || viewModel.isRebuilding)
                    }
                    .padding()
                    .background(Color(.systemBackground))
                    .cornerRadius(12)
                }
            }
            .padding()
        }
    }
}

struct InfoRow: View {
    let label: String
    let value: String
    
    var body: some View {
        HStack {
            Text(label)
                .foregroundColor(.secondary)
            Spacer()
            Text(value)
                .fontWeight(.medium)
        }
    }
}

// MARK: - Build Log Tab

struct BuildLogTab: View {
    @ObservedObject var viewModel: BuildDetailViewModel
    
    var body: some View {
        ZStack {
            Color(red: 0.12, green: 0.12, blue: 0.12)
                .ignoresSafeArea()
            
            if viewModel.isLoadingLog {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
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
                        proxy.scrollTo("logBottom", anchor: .bottom)
                    }
                }
                
                // Copy button
                VStack {
                    Spacer()
                    HStack {
                        Spacer()
                        Button {
                            UIPasteboard.general.string = logContent
                        } label: {
                            Image(systemName: "doc.on.doc")
                                .foregroundColor(.white)
                                .padding(12)
                                .background(Color.blue)
                                .clipShape(Circle())
                        }
                        .padding()
                    }
                }
            } else {
                Text("暂无日志")
                    .foregroundColor(.gray)
            }
        }
    }
}

// MARK: - Build Parameters Tab

struct BuildParametersTab: View {
    let parameters: [BuildParameter]
    
    var body: some View {
        if parameters.isEmpty {
            VStack(spacing: 12) {
                Image(systemName: "info.circle")
                    .font(.system(size: 48))
                    .foregroundColor(.secondary)
                Text("此构建没有参数")
                    .foregroundColor(.secondary)
            }
        } else {
            ScrollView {
                VStack(spacing: 0) {
                    ForEach(parameters) { param in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(param.name)
                                .font(.caption)
                                .foregroundColor(.blue)
                            Text(param.value ?? "(空)")
                                .font(.body)
                                .foregroundColor(param.value == nil ? .secondary : .primary)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding()
                        
                        if param.id != parameters.last?.id {
                            Divider()
                                .padding(.leading)
                        }
                    }
                }
                .background(Color(.systemBackground))
                .cornerRadius(12)
                .padding()
            }
        }
    }
}

// MARK: - ViewModel

@MainActor
class BuildDetailViewModel: ObservableObject {
    @Published var buildDetail: BuildDetailResponse?
    @Published var logContent: String?
    @Published var isLoading = false
    @Published var isLoadingLog = false
    @Published var isDeleting = false
    @Published var isRebuilding = false
    @Published var showError = false
    @Published var errorMessage: String?
    @Published var showActionAlert = false
    @Published var actionMessage: String?
    @Published var showDeleteConfirm = false
    @Published var showRebuildSheet = false
    @Published var rebuildParameters: [ParameterDefinition] = []
    @Published var shouldDismiss = false
    
    let jobName: String
    let jobURL: String
    let buildNumber: Int
    let buildURL: String
    
    private let api = JenkinsAPI.shared
    
    init(jobName: String, jobURL: String, buildNumber: Int, buildURL: String) {
        self.jobName = jobName
        self.jobURL = jobURL
        self.buildNumber = buildNumber
        self.buildURL = buildURL
    }
    
    func loadBuildDetail() async {
        isLoading = true
        
        do {
            buildDetail = try await api.fetchBuildDetailByURL(buildURL: buildURL)
        } catch let error as APIError {
            errorMessage = error.localizedDescription
            showError = true
        } catch {
            errorMessage = error.localizedDescription
            showError = true
        }
        
        isLoading = false
    }
    
    func loadLog() async {
        isLoadingLog = true
        
        do {
            logContent = try await api.fetchConsoleOutputByURL(buildURL: buildURL)
        } catch {
            // Silent fail for log loading
        }
        
        isLoadingLog = false
    }
    
    func showRebuildDialog() {
        Task {
            do {
                let jobDetail = try await api.fetchJobDetailByURL(jobURL: jobURL)
                if jobDetail.hasParameters {
                    // Pre-fill with current build's parameters
                    let currentParams = buildDetail?.buildParameters ?? []
                    rebuildParameters = jobDetail.parameterDefinitions.map { def in
                        if let currentValue = currentParams.first(where: { $0.name == def.name })?.value {
                            return ParameterDefinition(
                                name: def.name,
                                type: def.type,
                                description: def.description,
                                defaultParameterValue: ParameterValue(name: def.name, value: currentValue),
                                choices: def.choices
                            )
                        }
                        return def
                    }
                    showRebuildSheet = true
                } else {
                    // No parameters, rebuild directly
                    rebuild(with: [:])
                }
            } catch {
                actionMessage = "获取参数失败: \(error.localizedDescription)"
                showActionAlert = true
            }
        }
    }
    
    func rebuild(with parameters: [String: String]) {
        Task {
            isRebuilding = true
            showRebuildSheet = false
            
            do {
                try await api.triggerBuildByURL(jobURL: jobURL, parameters: parameters)
                actionMessage = "重新构建已触发"
                showActionAlert = true
            } catch let error as APIError {
                actionMessage = "重新构建失败: \(error.localizedDescription)"
                showActionAlert = true
            } catch {
                actionMessage = "重新构建失败: \(error.localizedDescription)"
                showActionAlert = true
            }
            
            isRebuilding = false
        }
    }
    
    func deleteBuild() async {
        isDeleting = true
        
        do {
            try await api.deleteBuild(buildURL: buildURL)
            actionMessage = "删除成功"
            shouldDismiss = true
            showActionAlert = true
        } catch let error as APIError {
            actionMessage = "删除失败: \(error.localizedDescription)"
            showActionAlert = true
        } catch {
            actionMessage = "删除失败: \(error.localizedDescription)"
            showActionAlert = true
        }
        
        isDeleting = false
    }
}


#Preview {
    BuildDetailView(
        jobName: "test-job",
        jobURL: "http://example.com/job/test/",
        buildNumber: 123,
        buildURL: "http://example.com/job/test/123/"
    )
}
