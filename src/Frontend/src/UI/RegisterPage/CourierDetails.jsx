export default function CourierDetails({formData, handleChange}) {
    return(
        <>
        <div className="form-grid">
            <label className="input-label">
                <span>Car make</span>
                <input
                className="register-input"
                type="text"
                name="carMake"
                value={formData.carMake}
                onChange={handleChange}
                placeholder="Enter car make"
                />
            </label>

            <label className="input-label">
                <span>Car model</span>
                <input
                className="register-input"
                type="text"
                name="carModel"
                value={formData.carModel}
                onChange={handleChange}
                placeholder="Enter car model"
                />
            </label>

            <label className="input-label">
                <span>Car year</span>
                <input
                className="register-input"
                type="number"
                name="carYear"
                value={formData.carYear}
                onChange={handleChange}
                placeholder="Enter car year"
                />
            </label>

            <label className="input-label">
                <span>Car color</span>
                <input
                className="register-input"
                type="text"
                name="carColor"
                value={formData.carColor}
                onChange={handleChange}
                placeholder="Enter car color"
                />
            </label>
        </div>
        </>
    )
}