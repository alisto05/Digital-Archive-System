import streamlit as st
from nav import nav_bar, footer, load_css, img_src

nav_bar()
load_css("assets/home.css")
st.session_state.selected_role = None

st.logo("assets/logo.png", size="large", icon_image="assets/logo.png")

# NOTE: no blank lines and no indentation inside this HTML,
# otherwise Streamlit's markdown turns it into a code block.
st.markdown(f"""
<div class="sp-home">
<section class="hero">
<h1>Welcome to SyncPoint</h1>
<img src="{img_src('assets/logo-removebg.png')}" alt="SyncPoint Hospital Logo" class="hero-logo">
<h2>Hospital Digital Archive System</h2>
<p>Hospital operations by improving data accessibility, reducing risks of lost or misplaced records, and enabling efficient reporting for hospital administration.</p>
</section>
<hr class="divider">
<section class="steps-section">
<h2>Manage your health record in 3 simple steps.</h2>
<p class="subtitle">Upload, search and access your documents</p>
<div class="steps-grid">
<div class="step-card">
<div class="caption">STEP 01</div>
<div class="img-wrapper"><img src="{img_src('assets/Upload-logo.png')}" alt="Upload Icon"></div>
<h3>Upload</h3>
<p>Upload your document securely</p>
</div>
<div class="step-card">
<div class="caption">STEP 02</div>
<div class="img-wrapper"><img src="{img_src('assets/search.png')}" alt="Search Icon"></div>
<h3>Search</h3>
<p>Search your documents that you uploaded</p>
</div>
<div class="step-card">
<div class="caption">STEP 03</div>
<div class="img-wrapper"><img src="{img_src('assets/download.png')}" alt="View/Download Icon"></div>
<h3>View/ Download</h3>
<p>Access your documents</p>
</div>
</div>
</section>
<hr class="divider">
</div>
""", unsafe_allow_html=True)

footer()
