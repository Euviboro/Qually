import React from "react";

export class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error, info) {
    // Swap this for a real logging service (Sentry, etc.) later
    console.error("[ErrorBoundary]", error, info.componentStack);
  }

  handleReset = () => {
    this.setState({ error: null });
  };

  render() {
    if (this.state.error) {
      return (
        <div className="flex flex-col items-center justify-center h-[60vh] gap-4 text-center px-8">
          <p className="text-5xl font-bold text-gray-200">Oops</p>
          <p className="text-gray-600 font-medium">Something went wrong on this page.</p>
          <p className="text-sm text-gray-400 font-mono bg-gray-50 px-4 py-2 rounded-lg max-w-md">
            {this.state.error.message}
          </p>
          <div className="flex gap-3 mt-2">
            <button
              onClick={this.handleReset}
              className="px-4 py-2 text-sm font-semibold bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
            >
              Try again
            </button>
            <button
              onClick={() => window.location.replace("/")}
              className="px-4 py-2 text-sm font-semibold bg-gray-100 text-gray-700 rounded-md hover:bg-gray-200 transition-colors"
            >
              Back to Dashboard
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}