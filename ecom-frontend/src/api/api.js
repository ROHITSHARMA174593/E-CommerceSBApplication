import axios from "axios";

const api = axios.create({
    baseURL: import.meta.env.MODE === "development" ? "http://localhost:5000/api" : `${import.meta.env.VITE_BACK_END_URL}/api`,
    // baseURL: import.meta.env.VITE_BACK_END_URL,
    withCredentials: true,
});

export default api;