import Foundation

class Console {
    fileprivate enum LogLevel: String {
        case debug = "🔵 [DEBUG]"
        case info = "😃 [INFO]"
        case warning = "😤 [WARNING]"
        case error = "😡 [ERROR]"
    }
    
    class func d(_ message: Any..., function: String = #function, file: String = #file, line: Int = #line) {
        #if DEBUG
        Console.write(loglevel: .debug, message: message, function: function, file: file, line: line)
        #endif
    }
    
    class func i(_ message: Any..., function: String = #function, file: String = #file, line: Int = #line ) {
        #if DEBUG
        Console.write(loglevel: .info, message: message, function: function, file: file, line: line)
        #endif
    }
    
    class func e(_ message: Any..., function: String = #function, file: String = #file, line: Int = #line ) {
        #if DEBUG
        Console.write(loglevel: .error, message: message, function: function, file: file, line: line)
        #endif
    }
    
    class func w(_ message: Any..., function: String = #function, file: String = #file, line: Int = #line ) {
        #if DEBUG
        Console.write(loglevel: .warning, message: message, function: function, file: file, line: line)
        #endif
    }
        
    class fileprivate func write(loglevel: LogLevel, message: Any, function: String, file: String, line: Int) {
        let fileName = Console.fileName(file: file)
        let queue = Thread.isMainThread ? "UI" : "BG"
        print("\(loglevel.rawValue) (\(queue)) : \(String(reflecting: message)) \(fileName).\(function):\(line)")
    }
    
    class fileprivate func fileName(file: String) -> String {
        var fileName = file
        if let match = fileName.range(of: "[^/]*swift", options: .regularExpression) {
            fileName = String(fileName[match])
        }
        
        return fileName
    }
}