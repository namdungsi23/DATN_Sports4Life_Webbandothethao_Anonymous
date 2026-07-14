function showBox() {
	const loginBox = document.getElementById("loginBox");
	const registerBox = document.getElementById("registerBox");
	
	//Toggle auth boxes
	loginBox.classList.toggle("d-none");
	registerBox.classList.toggle("d-none");
	
	// Check which is visible
	if (loginBox.classList.contains("d-none")) {
		authTitle.innerText = "Đăng ký";
	} else {
		authTitle.innerText = "Đăng nhập";
	}
}

const validateForm = () =>{
	const username = document.querySelector("#registerBox input[name='username']").value.trim();
	const fullname = document.querySelector("#registerBox input[name='fullname']").value.trim();
	const email = document.querySelector("#registerBox input[name='email']").value.trim();
	const password = document.querySelector("#registerBox input[name='password']").value;
	
	console.log(username);
	
	const usernameRegex = /^[a-zA-Z\d]{3,20}$/;
	const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
	const fullnameRegex = /^[\p{L}\s]{3,50}$/u;
	//const passwordRegex = /^[\W]{1}[A-Z]{1}.{6,}$/
	const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_]).{8,}$/;
	
	// Clear previous errors
    const errorContainer = document.querySelector("#registerBox .alert-danger");
    if (errorContainer) errorContainer.remove();

    // Validation
    let errorMessage = "";

    if (!usernameRegex.test(username)) {
        errorMessage = "Tên đăng nhập không hợp lệ (3-20 ký tự, chữ và số).";
    } else if (!fullnameRegex.test(fullname)) {
        errorMessage = "Họ tên chỉ chứa chữ cái và khoảng trắng (3-50 ký tự).";
    } else if (!emailRegex.test(email)) {
        errorMessage = "Email không hợp lệ.";
    } else if (!passwordRegex.test(password)) {
        errorMessage = "Mật khẩu phải có chữ hoa, chữ thường, số, ký tự đặc biệt và tối thiểu 8 ký tự.";
    } 

    if (errorMessage) {
        const formBody = document.querySelector("#registerBox");
        const div = document.createElement("div");
        div.className = "alert alert-danger text-center py-2";
        div.textContent = errorMessage;
        formBody.prepend(div);
        return false; 
    }

    return true; 
	
}

const passwordInput = document.querySelector("input[name='password']");
const confirmInput = document.querySelector("input[name='confirm']");
const errorDiv = document.getElementById("confirmError");

confirmInput.addEventListener("input", function () {
    const password = passwordInput.value;
    const confirm = confirmInput.value;

    if (confirm === "") {
        // Hide when empty
        errorDiv.classList.add("d-none");
        confirmInput.classList.remove("is-invalid");
        return;
    }

	if (password !== confirm) {
	    errorDiv.classList.remove("d-none");
	    confirmInput.classList.add("is-invalid");
	    confirmInput.classList.remove("is-valid");
	} else {
	    errorDiv.classList.add("d-none");
	    confirmInput.classList.remove("is-invalid");
	    confirmInput.classList.add("is-valid");
	}
});