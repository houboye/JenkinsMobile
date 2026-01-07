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

// MARK: - Crumb Response
struct CrumbResponse: Codable {
    let crumb: String
    let crumbRequestField: String
}

@MainActor
class JenkinsAPI: ObservableObject {
    static let shared = JenkinsAPI()
    
    @Published var isLoading = false
    
    private var server: Server?
    private var cachedCrumb: CrumbResponse?
    
    private init() {}
    
    func configure(with server: Server) {
        self.server = server
        self.cachedCrumb = nil  // Clear crumb when server changes
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
        let query = "tree=jobs[name,url,color,lastBuild[number,url],lastSuccessfulBuild[number,url],lastFailedBuild[number,url],healthReport[description,score],buildable]"
        let data = try await fetch(path: "api/json", query: query)
        let response = try decode(JobsResponse.self, from: data)
        return response.jobs
    }
    
    func fetchJobDetail(jobName: String) async throws -> JobDetailResponse {
        let encodedName = jobName.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? jobName
        let path = "job/\(encodedName)/api/json"
        let query = "tree=name,url,color,description,buildable,builds[number,url,result,timestamp,duration,displayName,building,description],lastBuild[number,url],lastSuccessfulBuild[number,url],lastFailedBuild[number,url],property[parameterDefinitions[name,type,description,defaultParameterValue[name,value],choices]]"
        let data = try await fetch(path: path, query: query)
        return try decode(JobDetailResponse.self, from: data)
    }
    
    func fetchJobDetailByURL(jobURL: String) async throws -> JobDetailResponse {
        let query = "tree=name,url,color,description,buildable,builds[number,url,result,timestamp,duration,displayName,building,description],lastBuild[number,url],lastSuccessfulBuild[number,url],lastFailedBuild[number,url],property[parameterDefinitions[name,type,description,defaultParameterValue[name,value],choices]]"
        let data = try await fetchByURL(jobURL: jobURL, suffix: "api/json", query: query)
        return try decode(JobDetailResponse.self, from: data)
    }
    
    func fetchBuild(jobName: String, buildNumber: Int) async throws -> Build {
        let encodedName = jobName.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? jobName
        let path = "job/\(encodedName)/\(buildNumber)/api/json"
        let data = try await fetch(path: path)
        return try decode(Build.self, from: data)
    }
    
    func triggerBuild(jobName: String, parameters: [String: String]? = nil) async throws {
        let encodedName = jobName.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? jobName
        // Use buildWithParameters for both parameterized and non-parameterized jobs
        let path = "job/\(encodedName)/buildWithParameters"
        try await post(path: path, parameters: parameters)
    }
    
    func triggerBuildByURL(jobURL: String, parameters: [String: String]? = nil) async throws {
        // Use buildWithParameters for both parameterized and non-parameterized jobs
        try await postByURL(jobURL: jobURL, suffix: "buildWithParameters", parameters: parameters)
    }
    
    /// Fetch CSRF crumb token from Jenkins
    func fetchCrumb() async throws -> CrumbResponse {
        // Return cached crumb if available
        if let cached = cachedCrumb {
            return cached
        }
        
        let data = try await fetch(path: "crumbIssuer/api/json")
        let crumb = try decode(CrumbResponse.self, from: data)
        cachedCrumb = crumb
        return crumb
    }
    
    func fetchConsoleOutput(jobName: String, buildNumber: Int) async throws -> String {
        let encodedName = jobName.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? jobName
        let path = "job/\(encodedName)/\(buildNumber)/consoleText"
        let data = try await fetch(path: path, acceptJSON: false)
        return String(data: data, encoding: .utf8) ?? ""
    }
    
    func fetchConsoleOutputByURL(buildURL: String) async throws -> String {
        let data = try await fetchByURL(jobURL: buildURL, suffix: "consoleText", acceptJSON: false)
        return String(data: data, encoding: .utf8) ?? ""
    }
    
    // MARK: - Private Methods
    
    /// Parse query string into URLQueryItem array
    private func parseQueryString(_ query: String) -> [URLQueryItem] {
        query.split(separator: "&").compactMap { pair in
            let parts = pair.split(separator: "=", maxSplits: 1)
            guard parts.count == 2 else { return nil }
            return URLQueryItem(name: String(parts[0]), value: String(parts[1]))
        }
    }
    
    /// Build URL using URLComponents for proper URL construction
    private func buildURL(baseURLString: String, path: String? = nil, query: String? = nil) throws -> URL {
        guard var components = URLComponents(string: baseURLString) else {
            throw APIError.invalidURL
        }
        
        // Append path if provided
        if let path = path {
            var currentPath = components.path
            if !currentPath.hasSuffix("/") {
                currentPath += "/"
            }
            currentPath += path
            components.path = currentPath
        }
        
        // Parse and set query items (URLQueryItem handles encoding properly)
        if let query = query {
            components.queryItems = parseQueryString(query)
        }
        
        guard let url = components.url else {
            throw APIError.invalidURL
        }
        return url
    }
    
    private func fetch(path: String, query: String? = nil, acceptJSON: Bool = true) async throws -> Data {
        guard let server = server else { throw APIError.invalidURL }
        guard let baseURL = server.baseURL else { throw APIError.invalidURL }
        
        let url = try buildURL(baseURLString: baseURL.absoluteString, path: path, query: query)
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue(server.authHeader, forHTTPHeaderField: "Authorization")
        if acceptJSON {
            request.setValue("application/json", forHTTPHeaderField: "Accept")
        }
        request.timeoutInterval = 30
        
        #if DEBUG
        print("[JenkinsAPI] GET \(url.absoluteString)")
        #endif
        
        return try await performRequest(request)
    }
    
    private func fetchByURL(jobURL: String, suffix: String, query: String? = nil, acceptJSON: Bool = true) async throws -> Data {
        guard let server = server else { throw APIError.invalidURL }
        
        let url = try buildURL(baseURLString: jobURL, path: suffix, query: query)
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue(server.authHeader, forHTTPHeaderField: "Authorization")
        if acceptJSON {
            request.setValue("application/json", forHTTPHeaderField: "Accept")
        }
        request.timeoutInterval = 30
        
        #if DEBUG
        print("[JenkinsAPI] GET \(url.absoluteString)")
        #endif
        
        return try await performRequest(request)
    }
    
    private func post(path: String, parameters: [String: String]? = nil) async throws {
        guard let server = server else { throw APIError.invalidURL }
        guard let baseURL = server.baseURL else { throw APIError.invalidURL }
        
        let url = try buildURL(baseURLString: baseURL.absoluteString, path: path)
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue(server.authHeader, forHTTPHeaderField: "Authorization")
        request.timeoutInterval = 30
        
        // Add CSRF crumb if available
        if let crumb = try? await fetchCrumb() {
            request.setValue(crumb.crumb, forHTTPHeaderField: crumb.crumbRequestField)
        }
        
        // Add parameters as form data
        if let parameters = parameters, !parameters.isEmpty {
            request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
            let formData = parameters.map { "\($0.key)=\($0.value.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? $0.value)" }.joined(separator: "&")
            request.httpBody = formData.data(using: .utf8)
        }
        
        #if DEBUG
        print("[JenkinsAPI] POST \(url.absoluteString)")
        #endif
        
        _ = try await performRequest(request, allowRedirect: true)
    }
    
    private func postByURL(jobURL: String, suffix: String, parameters: [String: String]? = nil) async throws {
        guard let server = server else { throw APIError.invalidURL }
        
        let url = try buildURL(baseURLString: jobURL, path: suffix)
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue(server.authHeader, forHTTPHeaderField: "Authorization")
        request.timeoutInterval = 30
        
        // Add CSRF crumb if available
        if let crumb = try? await fetchCrumb() {
            request.setValue(crumb.crumb, forHTTPHeaderField: crumb.crumbRequestField)
        }
        
        // Add parameters as form data
        if let parameters = parameters, !parameters.isEmpty {
            request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
            let formData = parameters.map { "\($0.key)=\($0.value.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? $0.value)" }.joined(separator: "&")
            request.httpBody = formData.data(using: .utf8)
        }
        
        #if DEBUG
        print("[JenkinsAPI] POST \(url.absoluteString)")
        #endif
        
        _ = try await performRequest(request, allowRedirect: true)
    }
    
    private func performRequest(_ request: URLRequest, allowRedirect: Bool = false) async throws -> Data {
        do {
            let (data, response) = try await URLSession.shared.data(for: request)
            
            guard let httpResponse = response as? HTTPURLResponse else {
                throw APIError.invalidResponse
            }
            
            switch httpResponse.statusCode {
            case 200...299:
                return data
            case 201 where allowRedirect, 302 where allowRedirect:
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
    
    private func decode<T: Decodable>(_ type: T.Type, from data: Data) throws -> T {
        do {
            let decoder = JSONDecoder()
            return try decoder.decode(type, from: data)
        } catch {
            throw APIError.decodingError(error)
        }
    }
}

