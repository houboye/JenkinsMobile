//
//  JenkinsAPI.swift
//  ios
//
//  Jenkins REST API service
//

import Foundation

enum APIError: Error, LocalizedError {
    case invalidURL
    case invalidResponse
    case unauthorized
    case serverError(Int)
    case decodingError(Error)
    case networkError(Error)
    
    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "无效的URL"
        case .invalidResponse:
            return "无效的服务器响应"
        case .unauthorized:
            return "认证失败，请检查用户名和API Token"
        case .serverError(let code):
            return "服务器错误: \(code)"
        case .decodingError(let error):
            return "数据解析错误: \(error.localizedDescription)"
        case .networkError(let error):
            return "网络错误: \(error.localizedDescription)"
        }
    }
}

@MainActor
class JenkinsAPI: ObservableObject {
    static let shared = JenkinsAPI()
    
    @Published var isLoading = false
    
    private var server: Server?
    
    private init() {}
    
    func configure(with server: Server) {
        self.server = server
    }
    
    // MARK: - API Methods
    
    func validateConnection(server: Server) async throws -> Bool {
        let url = server.baseURL?.appendingPathComponent("api/json")
        guard let url = url else { throw APIError.invalidURL }
        
        var request = URLRequest(url: url)
        request.setValue(server.authHeader, forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        
        let (_, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw APIError.invalidResponse
        }
        
        switch httpResponse.statusCode {
        case 200...299:
            return true
        case 401, 403:
            throw APIError.unauthorized
        default:
            throw APIError.serverError(httpResponse.statusCode)
        }
    }
    
    func fetchViews() async throws -> [JenkinsView] {
        let data = try await fetch(path: "api/json")
        let response = try decode(JenkinsRootResponse.self, from: data)
        return response.views
    }
    
    func fetchViewJobs(viewName: String) async throws -> [Job] {
        let encodedName = viewName.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? viewName
        let path = "view/\(encodedName)/api/json"
        let data = try await fetch(path: path)
        let response = try decode(ViewDetailResponse.self, from: data)
        return response.jobs
    }
    
    func fetchAllJobs() async throws -> [Job] {
        let data = try await fetch(path: "api/json?tree=jobs[name,url,color,lastBuild[number,url],lastSuccessfulBuild[number,url],lastFailedBuild[number,url],healthReport[description,score],buildable]")
        let response = try decode(JobsResponse.self, from: data)
        return response.jobs
    }
    
    func fetchJobDetail(jobName: String) async throws -> JobDetailResponse {
        let encodedName = jobName.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? jobName
        let path = "job/\(encodedName)/api/json?tree=name,url,color,description,buildable,builds[number,url,result,timestamp,duration,displayName,building,description],lastBuild[number,url],lastSuccessfulBuild[number,url],lastFailedBuild[number,url]"
        let data = try await fetch(path: path)
        return try decode(JobDetailResponse.self, from: data)
    }
    
    func fetchBuild(jobName: String, buildNumber: Int) async throws -> Build {
        let encodedName = jobName.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? jobName
        let path = "job/\(encodedName)/\(buildNumber)/api/json"
        let data = try await fetch(path: path)
        return try decode(Build.self, from: data)
    }
    
    func triggerBuild(jobName: String) async throws {
        let encodedName = jobName.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? jobName
        let path = "job/\(encodedName)/build"
        try await post(path: path)
    }
    
    func fetchConsoleOutput(jobName: String, buildNumber: Int) async throws -> String {
        let encodedName = jobName.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? jobName
        let path = "job/\(encodedName)/\(buildNumber)/consoleText"
        let data = try await fetch(path: path, acceptJSON: false)
        return String(data: data, encoding: .utf8) ?? ""
    }
    
    // MARK: - Private Methods
    
    private func fetch(path: String, acceptJSON: Bool = true) async throws -> Data {
        guard let server = server else { throw APIError.invalidURL }
        guard let baseURL = server.baseURL else { throw APIError.invalidURL }
        
        let url = baseURL.appendingPathComponent(path)
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue(server.authHeader, forHTTPHeaderField: "Authorization")
        if acceptJSON {
            request.setValue("application/json", forHTTPHeaderField: "Accept")
        }
        request.timeoutInterval = 30
        
        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            
            guard let httpResponse = response as? HTTPURLResponse else {
                throw APIError.invalidResponse
            }
            
            switch httpResponse.statusCode {
            case 200...299:
                return data
            case 401, 403:
                throw APIError.unauthorized
            default:
                throw APIError.serverError(httpResponse.statusCode)
            }
        } catch let error as APIError {
            throw error
        } catch {
            throw APIError.networkError(error)
        }
    }
    
    private func post(path: String) async throws {
        guard let server = server else { throw APIError.invalidURL }
        guard let baseURL = server.baseURL else { throw APIError.invalidURL }
        
        let url = baseURL.appendingPathComponent(path)
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue(server.authHeader, forHTTPHeaderField: "Authorization")
        request.timeoutInterval = 30
        
        do {
            let (_, response) = try await URLSession.shared.data(for: request)
            
            guard let httpResponse = response as? HTTPURLResponse else {
                throw APIError.invalidResponse
            }
            
            switch httpResponse.statusCode {
            case 200...299, 201, 302:
                return
            case 401, 403:
                throw APIError.unauthorized
            default:
                throw APIError.serverError(httpResponse.statusCode)
            }
        } catch let error as APIError {
            throw error
        } catch {
            throw APIError.networkError(error)
        }
    }
    
    private func decode<T: Decodable>(_ type: T.Type, from data: Data) throws -> T {
        do {
            let decoder = JSONDecoder()
            return try decoder.decode(type, from: data)
        } catch {
            throw APIError.decodingError(error)
        }
    }
}

