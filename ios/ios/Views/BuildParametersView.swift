//
//  BuildParametersView.swift
//  ios
//
//  View for inputting build parameters before triggering a build
//

import SwiftUI

struct BuildParametersView: View {
    let jobName: String
    let parameters: [ParameterDefinition]
    let onBuild: ([String: String]) -> Void
    let onCancel: () -> Void
    
    @State private var parameterValues: [String: String] = [:]
    @Environment(\.dismiss) private var dismiss
    
    init(jobName: String, parameters: [ParameterDefinition], onBuild: @escaping ([String: String]) -> Void, onCancel: @escaping () -> Void) {
        self.jobName = jobName
        self.parameters = parameters
        self.onBuild = onBuild
        self.onCancel = onCancel
        
        // Initialize with default values
        var defaults: [String: String] = [:]
        for param in parameters {
            defaults[param.name] = param.defaultValue
        }
        _parameterValues = State(initialValue: defaults)
    }
    
    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text("需要填写以下参数来构建此任务")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                ForEach(parameters) { param in
                    Section(header: Text(param.name)) {
                        ParameterInputView(
                            parameter: param,
                            value: binding(for: param.name)
                        )
                        
                        if let description = param.description, !description.isEmpty {
                            Text(description)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
            .navigationTitle("构建参数")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") {
                        onCancel()
                        dismiss()
                    }
                }
                
                ToolbarItem(placement: .confirmationAction) {
                    Button("构建") {
                        onBuild(parameterValues)
                        dismiss()
                    }
                    .fontWeight(.semibold)
                }
            }
        }
    }
    
    private func binding(for key: String) -> Binding<String> {
        Binding(
            get: { parameterValues[key] ?? "" },
            set: { parameterValues[key] = $0 }
        )
    }
}

struct ParameterInputView: View {
    let parameter: ParameterDefinition
    @Binding var value: String
    
    var body: some View {
        switch parameter.parameterType {
        case .choice:
            choiceInput
        case .boolean:
            booleanInput
        case .text:
            textInput
        case .password:
            passwordInput
        case .string:
            stringInput
        }
    }
    
    @ViewBuilder
    private var choiceInput: some View {
        if let choices = parameter.choices, !choices.isEmpty {
            Picker("", selection: $value) {
                ForEach(choices, id: \.self) { choice in
                    Text(choice).tag(choice)
                }
            }
            .pickerStyle(.menu)
            .labelsHidden()
        } else {
            TextField("输入值", text: $value)
        }
    }
    
    @ViewBuilder
    private var booleanInput: some View {
        Toggle("", isOn: Binding(
            get: { value.lowercased() == "true" },
            set: { value = $0 ? "true" : "false" }
        ))
        .labelsHidden()
    }
    
    @ViewBuilder
    private var textInput: some View {
        TextEditor(text: $value)
            .frame(minHeight: 80)
    }
    
    @ViewBuilder
    private var passwordInput: some View {
        SecureField("输入密码", text: $value)
    }
    
    @ViewBuilder
    private var stringInput: some View {
        TextField("输入值", text: $value)
    }
}

#Preview {
    BuildParametersView(
        jobName: "ios_archive_multi_lines_m4_pro",
        parameters: [
            ParameterDefinition(
                name: "iOS_branch",
                type: "ChoiceParameterDefinition",
                description: "选择要构建的分支",
                defaultParameterValue: ParameterValue.preview(value: "origin/main"),
                choices: ["origin/main", "origin/develop", "origin/release"]
            ),
            ParameterDefinition(
                name: "buildConfig",
                type: "ChoiceParameterDefinition",
                description: "构建配置",
                defaultParameterValue: ParameterValue.preview(value: "staging"),
                choices: ["staging", "production"]
            ),
            ParameterDefinition(
                name: "enableDebug",
                type: "BooleanParameterDefinition",
                description: "是否启用调试模式",
                defaultParameterValue: ParameterValue.preview(value: "false"),
                choices: nil
            ),
            ParameterDefinition(
                name: "projectBuildNum",
                type: "StringParameterDefinition",
                description: "[可选项] 修改工程build number，如果不需要更新则不用勾选",
                defaultParameterValue: nil,
                choices: nil
            )
        ],
        onBuild: { params in
            print("Build with params: \(params)")
        },
        onCancel: {}
    )
}

// Preview helper
extension ParameterValue {
    static func preview(value: String) -> ParameterValue {
        let data = """
        {"name": "test", "value": "\(value)"}
        """.data(using: .utf8)!
        return try! JSONDecoder().decode(ParameterValue.self, from: data)
    }
}

