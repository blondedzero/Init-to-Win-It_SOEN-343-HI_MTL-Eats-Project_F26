export default function StoreDetails({formData, handleChange}) {
    return (
        <>
        <label className="input-label">
            <span>Store address</span>
            <input
            className="register-input"
            type="text"
            name="address"
            value={formData.address}
            onChange={handleChange}
            placeholder="Enter the store address"
            />
        </label>

        <label className="input-label">
            <span>Store name</span>
            <input
            className="register-input"
            type="text"
            name="storeName"
            value={formData.storeName}
            onChange={handleChange}
            placeholder="Enter the store name"
            />
        </label>
        </>
    )
}