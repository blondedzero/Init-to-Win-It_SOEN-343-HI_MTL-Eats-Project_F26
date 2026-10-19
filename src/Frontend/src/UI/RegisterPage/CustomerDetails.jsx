export default function CustomerDetails({ formData, handleChange }) {
    return (
        <>
            <label className="input-label">
            <span>Address</span>
            <input
                className="register-input"
                type="text"
                name="address"
                value={formData.address}
                onChange={handleChange}
                placeholder="123 Street, City, Province, Zip"
            />
            </label>
            <div className='payment-info'>
            <label className="input-label">
                <span>Card Number</span>
                <input
                className="register-input"
                type="text"
                name="cardNumber"
                value={getNumeric(formData.cardNumber)}
                onChange={handleChange}
                placeholder="1234 1234 1234 1234"
                />
            </label>
            <div className="card-details">
            <label className="input-label">
                <span>Exp</span>
                <div className='card-exp-date'>
                                    <input
                    className="register-input"
                    type="number"
                    name="cardExpMonth"
                    min={1}
                    max={12}
                    value={formData.cardExpMonth}
                    onChange={handleChange}
                    placeholder="MM"
                />
                <input
                    className="register-input"
                    type="number"
                    name="cardExpYear"
                    min={new Date().getFullYear() % 100}
                    max={99}
                    value={formData.cardExpYear}
                    onChange={handleChange}
                    placeholder="YY"
                />
                </div>
                </label>
                <label className="input-label">
                <span>CVV</span>
                <input
                    className="register-input"
                    type="number"
                    name="cardCVV"
                    value={formData.cardCVV}
                    onChange={handleChange}
                    placeholder="123"
                />
                </label>
            </div>                
            </div>
        </>
    )
}

function getNumeric(number) {
    return String(number).replace(/\D/g, '');
}